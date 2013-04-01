package com.sarah.Schnauzer.helper.DB.Redis;

import java.sql.ResultSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.sarah.Schnauzer.helper.DBConnectorConfig;
import com.sarah.Schnauzer.helper.Tags;
import com.sarah.Schnauzer.helper.DB.ISlaveDbHelper;
import com.sarah.Schnauzer.helper.DB.SlaveStatus;

public class RedisSlaveDBHelper implements ISlaveDbHelper{
	
	private static final Logger LOGGER = LoggerFactory.getLogger(RedisSlaveDBHelper.class);
	
	protected Jedis conn;
	protected DBConnectorConfig conConfig;

	@Override
	public boolean doOpen() {
		boolean isOpened = false;
		try {
			String binlog = conn.get(Tags.softname + ":" + Tags.binlog + ":" + conConfig.masterID);
			isOpened = true;
		} catch(Exception e) {
			isOpened = false;
		}
		if (isOpened) return true;
		try {
			conn = null;
			conn = new Jedis(conConfig.host, conConfig.port);
		}  catch(Exception e) {   
			LOGGER.error("Redis连接失败2[" + conConfig.host + ":" + conConfig.port + "]" + e.getMessage());
			return false;
		}
		return true;
	}

	@Override
	public ResultSet getRS(String sql) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean excuteSqlByTransaction(String[] sqlStr, String[] errInfo,
			boolean checkRowCount) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean excuteSqlByTransaction(String[] sqlStr, String[] errInfo) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean executeSql(String sql, String retInfo) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public SlaveStatus getSlaveStatus() {
		SlaveStatus result = null;
		try
		{
			String binlog = conn.get(Tags.softname + ":" + Tags.binlog + ":" + conConfig.masterID);
			int pos = Integer.parseInt(conn.get(Tags.softname + ":" + Tags.pos + ":" + conConfig.masterID));
			result = new SlaveStatus(binlog, pos, conConfig.masterID);
		} catch(Exception e) {
			return null;
		}
		return result;
	}

	@Override
	public ResultSet getTableFields(String TableName) {
		// TODO Auto-generated method stub
		return null;
	}

}