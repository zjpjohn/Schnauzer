/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sarah;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang.exception.NestableRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.code.or.OpenReplicator;
import com.google.code.or.listener.SocketGuard;
import com.sarah.tools.localinfo.*;
import com.sarah.tools.logging.Log4jInitializer;
import com.sarah.tools.logging.SetLogUtils;
import com.sarah.Schnauzer.heartbeat.HeartBeatInfo;
import com.sarah.Schnauzer.heartbeat.HeartBeatSender;
import com.sarah.Schnauzer.helper.ConfigGetHelper;
import com.sarah.Schnauzer.helper.DBConnectorConfig;
import com.sarah.Schnauzer.helper.Infos;
import com.sarah.Schnauzer.helper.Tags;
import com.sarah.Schnauzer.helper.DB.SlaveHelperFactory;
import com.sarah.Schnauzer.helper.DB.SlaveStatus;
import com.sarah.Schnauzer.listener.ClientTableListener;



/**
 *  @author SarahCla
 */
public class SchnauzerRunner {
	//
	private static final Logger LOGGER = LoggerFactory.getLogger(SchnauzerRunner.class);
	public static String SchnauzerID = "1";

	
	private static boolean iniRepStatus(DBConnectorConfig masterConfig, DBConnectorConfig slaveConfig) throws Exception {
		SlaveStatus rsSlave = (new SlaveHelperFactory(slaveConfig)).getSlaveStatus();
		if (rsSlave==null)	throw new Exception(Infos.ConSlave + Infos.Failed);

		masterConfig.binlog = rsSlave.binlog;
		masterConfig.pos = rsSlave.pos;
		slaveConfig.binlog = rsSlave.binlog;
		slaveConfig.pos = rsSlave.pos;
		
		LOGGER.info(Infos.GetRepStatus + Infos.OK + "  " + masterConfig.binlog + ":" + masterConfig.pos);
		return true;
	}
	
	
	private static void iniHeartBeat(HeartBeatSender beatSender, DBConnectorConfig masterConfig, DBConnectorConfig slaveConfig, ConfigGetHelper conf, LocalInfo info) {
		HeartBeatInfo hinfo = new HeartBeatInfo();
		conf.getHeartBeatSet(hinfo);			
		if (hinfo.port>0 && !hinfo.host.isEmpty()) {
			LOGGER.info(Infos.Start + Infos.SendHeartBeat);
			hinfo.SerialNo = info.getSNStr();
			beatSender = new HeartBeatSender(masterConfig, slaveConfig, hinfo);
			beatSender.start();
		}		
	}
	
	private static void doRep(String args[]) {
		LocalInfo info = LocalInfoGetter.getLocalInfo();
		DBConnectorConfig masterConfig = new DBConnectorConfig();
		DBConnectorConfig slaveConfig = new DBConnectorConfig(info);
		LOGGER.info(Infos.SysInfo + ":" + info.toString());
		HeartBeatSender beatSender = null;
		try
		{

			ConfigGetHelper conf = new ConfigGetHelper(SchnauzerID);

			if (!conf.getDBConfig(masterConfig, Tags.MasterDB)) System.exit(-1);
			if (!conf.getDBConfig(slaveConfig, Tags.SlaveDB))  System.exit(-1);

			SlaveHelperFactory mHelper = new SlaveHelperFactory(masterConfig);
			ResultSet rsMaster = mHelper.getRS("select 1");
			if (rsMaster==null)	throw new Exception(Infos.ConMaster + Infos.Failed);
			
			iniRepStatus(masterConfig, slaveConfig);
			iniHeartBeat(beatSender, masterConfig, slaveConfig, conf, info);
			
			final OpenReplicator or = new OpenReplicator();
			or.setMasterAndSlave(masterConfig, slaveConfig);
			ClientTableListener listener = new ClientTableListener(masterConfig, slaveConfig, args);
			or.setBinlogEventListener(listener);

			SocketGuard guard = new SocketGuard(or, mHelper.getMySQLHelper());
			guard.start();
			or.start();

			final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			for(String line = br.readLine(); line != null; line = br.readLine()) {
			    if(line.equals("q")) {
			        or.stop(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
			        break;
			    }
			}
		} catch (NestableRuntimeException e) {
			LOGGER.error("catch an exception");
			if (beatSender!=null) beatSender.stopRunning();			
		} catch (java.net.ConnectException e) {
			LOGGER.error(Infos.ConDB + Infos.Failed);
			if (beatSender!=null) beatSender.stopRunning();
		} catch (SQLException e) {
			LOGGER.error("catch an SQLException{}" + e);
			if (beatSender!=null) beatSender.stopRunning();
			e.printStackTrace();
		} catch (Exception e) {
			LOGGER.error("catch an Exception {}" + e);
			if (beatSender!=null) beatSender.stopRunning();
		}
	}
	/**
	 * 
	 */
	public static void main(String args[]) throws Exception {
		if (args.length>0) {
			SchnauzerID = args[0].trim();
		}
		System.out.println("Hi, pretty dog, your ID is " + SchnauzerID);
		SetLogUtils utils = new SetLogUtils();
		utils.setlogpath("LOG.INFO", "logs-" + SchnauzerID);
		Log4jInitializer.initialize("logs-" + SchnauzerID + ".xml");
		LOGGER.info(Infos.Started);
		LOGGER.info("[PublishedDate] " + Tags.PublishDate);
		while (true) 
		{
			try
			{
				doRep(args);	
			} catch (org.apache.commons.lang.exception.NestableRuntimeException e) {
				LOGGER.error("catch an exception");
			}
			LOGGER.info(Infos.NeedRetry);
			Thread.sleep(5*60*1000);
			LOGGER.info(Infos.Recon);
		}
	}
}
