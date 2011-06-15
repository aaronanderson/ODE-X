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
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;
import javax.xml.namespace.QName;

import org.apache.ode.spi.exec.ActionTask.ActionId;
import org.apache.ode.spi.exec.ActionTask.ActionMessage;
import org.apache.ode.spi.exec.ActionTask.ActionStatus;
import org.apache.ode.spi.exec.ActionTask.Status;
import org.w3c.dom.Document;

@Entity
@Table(name = "ACTION_STATUS")
public class ActionStatusImpl implements ActionStatus, Serializable {

	@Id
	@Column(name = "ACTION_ID")
	private String actionId;

	@Column(name = "NODE_ID")
	private String nodeId;

	@Column(name = "ACTION_TYPE")
	private String actionType;

	@Column(name = "ACTION_STATUS")
	private String status;

	@Column(name = "USER")
	private String user;

	@Version
	@Column(name = "LAST_MODIFIED")
	private Timestamp lastModified;

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public String getNodeId() {
		return nodeId;
	}

	public void setActionType(String actionType) {
		this.actionType = actionType;
	}

	public String getActionType() {
		return actionType;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Timestamp getLastModified() {
		return lastModified;
	}

	@Override
	public ActionId id() {
		return null;
	}

	@Override
	public QName type() {
		return null;
	}

	@Override
	public Status status() {
		return null;
	}

	@Override
	public List<ActionMessage> messages() {
		return null;
	}

	@Override
	public Document result() {
		return null;
	}

	@Override
	public Date start() {
		return null;
	}

	@Override
	public Date finish() {
		return null;
	}

}
