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
package com.sarah.Schnauzer.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sarah.Schnauzer.listener.TableReplicator.Redis.Fields.CheckField;

/**
* @author SarahCla
*/
public class ErrorHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(ErrorHelper.class);
	
	public static void errExit(String info) {
		WarmingMailHelper mailsender = new WarmingMailHelper("Config.xml");
		mailsender.send("【复制故障】", info);
		LOGGER.error("【复制故障】" + info);
		System.exit(-1);
	}
	
}
