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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

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

	@Column(name = "STATUS")
	private String status;

	@Version
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "HEARTBEAT")
	private Timestamp heartBeat;

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

	public void setStatus(Status status) {
		this.status = status.name();
	}

	public Status getStatus() {
		if (status != null) {
			return Status.valueOf(this.status);
		}
		return Status.INACTIVE;
	}

	public Timestamp getHeartBeat() {
		return heartBeat;
	}

	public static enum Status {
		ACTIVE, INACTIVE;
	}

}
