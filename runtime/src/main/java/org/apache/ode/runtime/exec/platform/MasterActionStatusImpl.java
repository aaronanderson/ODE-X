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

import java.io.Serializable;
import java.util.List;

import org.apache.ode.spi.exec.ActionTask.ActionMessage;
import org.apache.ode.spi.exec.MasterActionTask.MasterActionStatus;
import org.apache.ode.spi.exec.SlaveActionTask.SlaveActionStatus;

public class MasterActionStatusImpl extends ActionStatusImpl implements MasterActionStatus, Serializable {

	@Override
	public List<SlaveActionStatus> slaveStatus() {
		return null;
	}

	
	

}
