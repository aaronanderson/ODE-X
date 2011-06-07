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

import org.apache.ode.spi.exec.Action.ActionId;
import org.apache.ode.spi.exec.ActionContext;
import org.apache.ode.spi.exec.ActionMessage;
import org.apache.ode.spi.exec.Platform;
import org.w3c.dom.Document;

public class ActionContextImpl implements ActionContext {

	private ActionId actionId;
	private String nodeId;
	private Document actionInfo;
	private Platform platform;
	private String state;

	public ActionContextImpl(ActionId actionId, String nodeId, Document actionInfo, Platform platform) {

	}

	public ActionId getActionId() {
		return actionId;
	}

	public String getNodeId() {
		return nodeId;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getState() {
		return state;
	}

	public void log(ActionMessage message) {

	}

	public Document getActionInfo() {
		return actionInfo;
	}

	public Platform getPlatform() {
		return platform;
	}
}
