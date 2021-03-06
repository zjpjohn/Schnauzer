package com.sarah.Schnauzer.listener.master.Impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.code.or.binlog.impl.event.DeleteRowsEvent;
import com.google.code.or.binlog.impl.event.UpdateRowsEvent;
import com.google.code.or.binlog.impl.event.WriteRowsEvent;
import com.google.code.or.common.glossary.Column;
import com.google.code.or.common.glossary.Pair;
import com.google.code.or.common.glossary.Row;
import com.sarah.SchnauzerRunner;
import com.sarah.Schnauzer.helper.ConfigGetHelper;
import com.sarah.Schnauzer.helper.DBConnectorConfig;
import com.sarah.Schnauzer.helper.ErrorHelper;
import com.sarah.Schnauzer.helper.Infos;
import com.sarah.Schnauzer.helper.DB.ISlaveDbHelper;
import com.sarah.Schnauzer.helper.DB.MasterDBHelper;
import com.sarah.Schnauzer.helper.DB.SlaveHelperFactory;
import com.sarah.Schnauzer.listener.ColumnTypeHelper;
import com.sarah.Schnauzer.listener.TableReplicator.RDB.Impl.RepField;
import com.sarah.Schnauzer.listener.TableReplicator.RDB.Impl.RDBSchnauzer;
import com.sarah.Schnauzer.listener.TableReplicator.RDB.Impl.SASRDBSchnauzer;
import com.sarah.Schnauzer.listener.master.AbstractRDBMaster;
import com.sarah.Schnauzer.listener.master.IMaster;


/**
 * 
 * @author SarahCla
 */
public class SchnauzerSASMaster extends AbstractRDBMaster implements IMaster {

	
	private static final Logger LOGGER = LoggerFactory.getLogger(SchnauzerSASMaster.class);	
	
	public SchnauzerSASMaster(DBConnectorConfig master, DBConnectorConfig slave) {
		super(master, slave);
	}
	
	private boolean setUnsignedLong(RDBSchnauzer table) {
		ISlaveDbHelper sdbhelper = new SlaveHelperFactory(this.slaveDb);
		try
		{
			String name = table.getSlaveTableName();
			ResultSet rs = sdbhelper.getTableFields(name);
			while(rs.next()) {
				if (this.slaveDb.isMySQL() && rs.getString("data_type").equalsIgnoreCase("bigint") && rs.getString("numeric_precision").equals("20")) 
					table.setUnsignedLong(rs.getString("column_name"));
				if (rs.getString("isText").equalsIgnoreCase("1")) 
					table.setSlaveTextField(rs.getString("column_name"));
			}
			rs.close();
		} catch (SQLException e) {
			LOGGER.error(Infos.Get + "Slave" + Infos.TableStructure + Infos.Failed + e.getMessage());
		}
		return false;
	}
	
	private void setCharacterSet(RepField field, ResultSet rs) throws SQLException {
		String characterset = rs.getString("character_set_name");
		if (characterset!=null && !characterset.equals("")) field.characterset = characterset;
	}
	
	private void setFieldUnsignedLong(RepField field, ResultSet rs) throws SQLException {
		if (rs.getString("data_type").equalsIgnoreCase("bigint") && rs.getString("numeric_precision").equals("20")) 
			field.isUnsignedLong = true;
	}
	
	@Override
	public boolean registgerTableReplicator(DBConnectorConfig master) {
		try
		{
			this.tables.clear();
			ConfigGetHelper conf = new ConfigGetHelper(SchnauzerRunner.SchnauzerID);
			conf.getRDBRepTables(this.tables);
			MasterDBHelper mdbhelper = new MasterDBHelper(master);
			getAllProfilor(mdbhelper);
			boolean pass = true;
			boolean haveCol = false;
			for (int i=0; i<this.tables.size(); i++)
			{
				SASRDBSchnauzer table = (SASRDBSchnauzer)this.tables.get(i);
				List<RepField> confFields = table.getConfFields();
				ResultSet rs = mdbhelper.getTableFields(table.getMasterTableName());
				haveCol = false;
				boolean isDiff = table.isHeterogenous();
				while(rs.next())
				{
					haveCol = true;
					RepField field = new RepField();
					String mFieldName = rs.getString("column_name");
					if (isDiff) 
					{
						RepField f = table.getConfField(mFieldName);
						field.copy(f);	
					} else {
						field.masterfield = mFieldName;
						field.slavefield = field.masterfield;
					}
					setFieldUnsignedLong(field, rs);
					setCharacterSet(field, rs);
					table.addFullField(field);
					if (table.checkfield.equalsIgnoreCase(mFieldName)) 
						table.checkFieldIndex = table.getFieldCount()-1;
				}
				if (!haveCol) {
					ErrorHelper.errExit(String.format(Infos.TableNotExist, table.getMasterTableName(), masterDb.dbname));
				}
				String defStr = "";
				for (int j=0; j<confFields.size(); j++)
				{
					RepField f = confFields.get(j); 
					if (!f.isNew) continue;
					RepField newfield = new RepField();
					newfield.copy(f);
					table.addFullField(newfield);
					if (newfield.isCuidDefValue) {
						table.cuidDefault = true;
						defStr += ",";
					}
					else
						defStr += "," + f.defvalue;
				}
				table.defValueStr = defStr;
				setUnsignedLong(table);
				table.setFieldNameTag(this.slaveDb.fieldTag[0], this.slaveDb.fieldTag[1]);
				LOGGER.info("{}", table.getFullFields());
				pass = pass && table.setIndex(); 
			}
			if (!pass) 
				ErrorHelper.errExit(Infos.GetTableConfigs + Infos.Failed);
		} catch (Exception e) {
			ErrorHelper.errExit(Infos.GetTableConfigs + Infos.Failed + e.toString());
		}
		return true;
	}

	private void delProfilor(String id) {
		LOGGER.info("Profilor=" + id + "  取消同步");
		for (int i=0; i<this.tables.size(); i++) {
			SASRDBSchnauzer table = (SASRDBSchnauzer)this.tables.get(i);
			table.deleteProfilor(id);
		}
	}

	private void addProfilor(String id) {
		LOGGER.info("Profilor=" + id + "  开始同步");
		for (int i=0; i<this.tables.size(); i++) {
			SASRDBSchnauzer table = (SASRDBSchnauzer)this.tables.get(i);
			table.addProfilor(id);
		}
	}
	
	private void clearProfilors() {
		for(int i=0; i<this.tables.size(); i++) {
			SASRDBSchnauzer table = (SASRDBSchnauzer)this.tables.get(i);
			table.clearProfilors();
		}
	}
	
	private void getAllProfilor(MasterDBHelper mdbhelper) {
		clearProfilors();
		try
		{
			ResultSet rs = mdbhelper.getProfilors(SchnauzerRunner.SchnauzerID);
			while(rs.next())
			{
				String id = rs.getString(1); 
				for(int i=0; i<this.tables.size(); i++) {
					SASRDBSchnauzer table = (SASRDBSchnauzer)this.tables.get(i);
					table.addProfilor(id);
				}
			}
			rs.close();
		}catch(Exception e) {
			
		}
	}
	
	@Override
	public boolean doBeforeWrite(ColumnTypeHelper helper, WriteRowsEvent event) {
		if (!helper.databaseName.equalsIgnoreCase("cloudmaster")) return true;
		if (!helper.tableName.equalsIgnoreCase("profilebindmodule")) return true;
		List<Row> rows = event.getRows();
		for (int i=0; i<rows.size(); i++) {
			Row row = rows.get(i);
			List<Column> columns = row.getColumns();
			String serverID = columns.get(2).toString();
			if (!serverID.equalsIgnoreCase(SchnauzerRunner.SchnauzerID)) continue;
			String canSyn = columns.get(4).toString();
			String profilor = columns.get(0).toString();
			if (canSyn.equalsIgnoreCase("1"))
				addProfilor(profilor);
		}
		return true;
	}

	@Override
	public boolean doBeforeUpdate(ColumnTypeHelper helper, UpdateRowsEvent event) {
		if (!helper.databaseName.equalsIgnoreCase("cloudmaster")) return true;
		if (!helper.tableName.equalsIgnoreCase("profilebindmodule")) return true;
		List<Pair<Row>> rows =  event.getRows();
		for (int i=0; i<rows.size(); i++) {
            Pair<Row> pRow = rows.get(i);  
            Row row = pRow.getAfter();
			List<Column> columns = row.getColumns();
			String serverID = columns.get(2).toString();
			if (!serverID.equalsIgnoreCase(SchnauzerRunner.SchnauzerID)) continue;
			String canSyn = columns.get(4).toString();
			String profilor = columns.get(0).toString();
			if (canSyn.equalsIgnoreCase("0"))
			{
				delProfilor(profilor);
			}else {
				addProfilor(profilor);
			}
		}
		return true;
	}

	@Override
	public boolean doBeforeDelete(ColumnTypeHelper helper, DeleteRowsEvent event) {
		if (!helper.databaseName.equalsIgnoreCase("cloudmaster")) return true;
		if (!helper.tableName.equalsIgnoreCase("profilebindmodule")) return true;
		List<Row> rows = event.getRows();
		for (int i=0; i<rows.size(); i++) {
			Row row = rows.get(i);
			List<Column> columns = row.getColumns();
			String serverID = columns.get(2).toString();
			if (!serverID.equalsIgnoreCase(SchnauzerRunner.SchnauzerID)) continue;
			String canSyn = columns.get(4).toString();
			String profilor = columns.get(0).toString();
			addProfilor(profilor);
		}
		return true;
	}


}
