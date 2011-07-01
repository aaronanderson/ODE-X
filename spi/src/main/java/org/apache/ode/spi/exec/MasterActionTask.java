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
package org.apache.ode.spi.exec;

import java.util.Set;

import org.apache.ode.spi.exec.ActionTask.ActionContext;
import org.apache.ode.spi.exec.MasterActionTask.MasterActionContext;
import org.w3c.dom.Document;

/**
 * 
 * Intended to run once only on a single node but coordinate with multiple slave
 * tasks across nodes
 * 
 */
public interface MasterActionTask extends ActionTask<MasterActionContext> {

	public interface MasterActionContext extends ActionContext {

		public Set<ActionStatus> slaveStatus();
		
		public void setInput(String nodeId, Document input) throws PlatformException;


	}

	public interface MasterActionStatus extends ActionStatus {

		public Set<ActionStatus> slaveStatus();

	}

}