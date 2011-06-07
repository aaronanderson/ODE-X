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
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

import org.apache.ode.spi.exec.ActionMessage;
import org.apache.ode.spi.exec.ActionStatus.NodeStatus;

@Entity
@Table(name = "CLUSTER_NODES")
public class NodeStatusImpl implements NodeStatus, Serializable {

	@Id
	@Column(name = "NODE_ID")
	private String nodeId;

	@Id
	@Column(name = "ACTION_ID")
	private String actionId;
	
	@Column(name = "STATUS")
	private String state;

	@Version
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "MODIFIED")
	private Timestamp modified;

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
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

	public Timestamp getModified() {
		return modified;
	}

	public static enum Status {
		ACTIVE, INACTIVE;
	}

	@Override
	public String nodeId() {
		return getNodeId();
	}

	@Override
	public String state() {
		return getState();
	}

	@Override
	public List<ActionMessage> messages() {
		return null;
	}

}
