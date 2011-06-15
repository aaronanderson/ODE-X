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

import java.util.concurrent.atomic.AtomicReference;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.ode.runtime.exec.cluster.xml.ActionCheck;
import org.apache.ode.spi.exec.NodeStatus.State;

public class ActionPoll implements Runnable {

	@PersistenceContext(unitName = "platform")
	private EntityManager pmgr;
	private String clusterId;
	private String nodeId;
	AtomicReference<State> localNodeState;
	ActionCheck config;

	@Override
	public synchronized void run() {

	}

	public void init(String clusterId, String nodeId, AtomicReference<State> localNodeState, ActionCheck config) {
		this.clusterId = clusterId;
		this.nodeId = nodeId;
		this.localNodeState = localNodeState;
		this.config = config;
	}

}
