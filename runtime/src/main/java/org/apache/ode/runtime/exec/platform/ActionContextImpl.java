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

import javax.xml.namespace.QName;

import org.apache.ode.spi.exec.ActionTask.ActionContext;
import org.apache.ode.spi.exec.ActionTask.ActionId;
import org.apache.ode.spi.exec.ActionTask.ActionMessage;
import org.apache.ode.spi.exec.ActionTask.Status;
import org.apache.ode.spi.exec.Platform;
import org.w3c.dom.Document;

public class ActionContextImpl implements ActionContext {

	private ActionId actionId;
	private QName type;
	private String nodeId;
	private Document actionInput;
	private Status status;

	public ActionContextImpl(ActionId actionId, QName type, String nodeId, Document actionInput, Platform platform) {

	}

	@Override
	public ActionId id() {
		return actionId;
	}

	@Override
	public QName type() {
		return type;
	}

	@Override
	public void log(ActionMessage message) {
	}
	
	@Override
	public Document input() {
		return actionInput;
	}

	@Override
	public void refresh() {
	}

	@Override
	public Status getStatus() {
		return status;
	}

	@Override
	public void updateStatus(Status status) {
	}

	@Override
	public void updateResult(Document result) {
	}

}
