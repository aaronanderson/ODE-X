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
package org.apache.ode.server.exec;

import java.util.Calendar;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;

import org.apache.ode.runtime.exec.cluster.xml.ClusterConfig;
import org.apache.ode.runtime.exec.platform.ActionExecutor;
import org.apache.ode.runtime.exec.platform.ActionPoll;
import org.apache.ode.runtime.exec.platform.HealthCheck;
import org.apache.ode.runtime.exec.platform.Node;
import org.apache.ode.spi.exec.NodeStatus;
import org.apache.ode.spi.exec.NodeStatus.NodeState;
import org.apache.ode.spi.exec.Platform;
import org.apache.ode.spi.repo.Repository;

@Singleton
public class ClusterAssistant {

	@PersistenceContext(unitName = "platform")
	private EntityManager pmgr;

	@Inject
	Repository repository;

	@Inject
	Platform platform;

	@Inject
	HealthCheck healthCheck;

	@Inject
	ActionPoll actionPoll;
	
	@Inject
	ActionExecutor actionExec;
	
	String clusterId;

	String nodeId;

	ClusterConfig config;

	AtomicReference<NodeState> nodeStatus = new AtomicReference<NodeState>();

	@PostConstruct
	public void init() {
		System.out.println("Initializing ClusterAssistant");
		System.out.println("ClusterAssistant Initialized");
	}

	public void setup(String nodeId, String clusterId, ClusterConfig config) {
		this.nodeId = nodeId;
		this.clusterId = clusterId;
		this.config = config;
		nodeStatus.set(NodeState.OFFLINE);
		healthCheck.init(clusterId, nodeId, nodeStatus, config.getHealthCheck());
		actionPoll.init(clusterId, nodeId, nodeStatus,actionExec, config.getActionCheck());
	}

	public void online() {
		nodeStatus.set(NodeState.ONLINE);
		healthCheck.run();
	}

	public void offline() {
		nodeStatus.set(NodeState.OFFLINE);
		healthCheck.run();
	}

	public Set<NodeStatus> availableNodes() {
		return healthCheck.availableNodes();
	}

	public void healthCheck() {
		healthCheck.run();
	}

	public Calendar getHeartBeat(String nodeId) {
		try {
			Node local = pmgr.find(Node.class, nodeId);
			return local.getHeartBeat();
		} catch (PersistenceException pe) {
			pe.printStackTrace();
			return null;
		} finally {
			pmgr.clear();
		}

	}

}
