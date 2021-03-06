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
package com.sarah.Schnauzer.listener.TableReplicator.Redis.Impl;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import com.sarah.Schnauzer.listener.TableReplicator.Redis.Fields.BaseField;
import com.sarah.Schnauzer.listener.TableReplicator.Redis.Fields.RedisStructure;

/**
 * 
 * @author SarahCla
 */
public class RedisRepTable {
	private List<BaseField> keyfields = new CopyOnWriteArrayList<BaseField>();
	public String MasterTable = "";
	public String SlaveKey = "";
	public RedisStructure type = null;
	public String SQL = "";
	
	public void setKeyFiels(String keyFields) {
		keyfields.clear();
		if (keyFields.equals("")) return;
		String[] fields = keyFields.split(",");
		for (int i=0; i<fields.length; i++) {
			BaseField field = new BaseField(fields[i]);
			keyfields.add(field);
		}
	}
	
	public List<BaseField> getKeyFields() {
		return keyfields;
	}
	
	public Boolean haveKeyFields() {
		return keyfields.size()>0;
	}
}
