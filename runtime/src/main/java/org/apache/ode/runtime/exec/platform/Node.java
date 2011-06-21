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
import java.util.Calendar;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

import org.apache.ode.spi.exec.NodeStatus.NodeState;

@NamedQueries({ @NamedQuery(name = "healthCheck", query = "select node from Node node where node.heartBeat > :lifetime"),
		@NamedQuery(name = "deleteDeadNodes", query = "delete from Node node where node.heartBeat < :lifetime") })
@Entity
@Table(name = "NODE")
public class Node implements Serializable {

	@Id
	@Column(name = "NODE_ID")
	private String nodeId;

	@Column(name = "CLUSTER_ID")
	private String clusterId;

	// re-calculated when a node goes down so servers have sequential order,
	// schedulers can be offset so they don't run at the same time
	@Column(name = "NODE_SEQ")
	private int nodeSequence;

	@Column(name = "STATE")
	private String state;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "HEARTBEAT")
	private Calendar heartBeat;

	@Version
	@Column(name = "VERSION")
	private long version;

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public String getNodeId() {
		return nodeId;
	}

	public void setClusterId(String clusterId) {
		this.clusterId = clusterId;
	}

	public String getClusterId() {
		return clusterId;
	}

	public void setNodeSequence(int nodeSequence) {
		this.nodeSequence = nodeSequence;
	}

	public int getNodeSequence() {
		return nodeSequence;
	}

	public void setState(NodeState state) {
		this.state = state.name();
	}

	public NodeState getState() {
		if (state != null) {
			return NodeState.valueOf(state);
		}
		return NodeState.OFFLINE;
	}

	public Calendar getHeartBeat() {
		return heartBeat;
	}

	public void setHeartBeat(Calendar heartBeat) {
		this.heartBeat = heartBeat;
	}

}
