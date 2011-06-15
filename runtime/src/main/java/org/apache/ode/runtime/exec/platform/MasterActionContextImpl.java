/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ode.runtime.exec.platform;

import java.util.List;

import javax.xml.namespace.QName;

import org.apache.ode.spi.exec.ActionTask.ActionId;
import org.apache.ode.spi.exec.MasterActionTask.MasterActionContext;
import org.apache.ode.spi.exec.Platform;
import org.apache.ode.spi.exec.SlaveActionTask.SlaveActionStatus;
import org.w3c.dom.Document;

public class MasterActionContextImpl extends ActionContextImpl implements MasterActionContext {


	public MasterActionContextImpl(ActionId actionId, QName type, String nodeId, Document actionInput, Platform platform) {
		super(actionId, type, nodeId, actionInput, platform);
	}

	@Override
	public List<SlaveActionStatus> slaveStatus() {
		// TODO Auto-generated method stub
		return null;
	}


}
