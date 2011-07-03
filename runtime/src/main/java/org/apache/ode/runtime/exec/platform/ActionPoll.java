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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.apache.ode.runtime.exec.cluster.xml.ActionCheck;
import org.apache.ode.runtime.exec.platform.ActionExecutor.ActionRunnable;
import org.apache.ode.spi.exec.PlatformException;
import org.apache.ode.spi.exec.ActionTask.ActionState;
import org.apache.ode.spi.exec.NodeStatus.NodeState;

public class ActionPoll implements Runnable {

	@PersistenceContext(unitName = "platform")
	private EntityManager pmgr;

	private ActionExecutor exec;
	private String clusterId;
	private String nodeId;
	AtomicReference<NodeState> localNodeState;
	ActionCheck config;

	@Override
	public synchronized void run() {
		try {// stop for nothing
			// Refresh the executing entries
			for (Iterator<ActionRunnable> i = exec.getExecutingTasks().values().iterator(); i.hasNext();) {
				ActionRunnable runnable = i.next();
				//System.out.format("refreshing: ActionId: %s\n", runnable.action.getActionId());
				runnable.pollUpdate();
			}
			// Spawn new tasks
			List<Action> newTasks = null;
			try {
				pmgr.clear();
				Query newTasksQuery = pmgr.createNamedQuery("localNewTasks");
				newTasksQuery.setParameter("nodeId", nodeId);
				newTasks = (List<Action>) newTasksQuery.getResultList();
			} catch (PersistenceException pe) {
				pe.printStackTrace();
			}
			if (newTasks != null) {
				for (Action a : newTasks) {
					if (!exec.getExecutingTasks().containsKey(a.getActionId())) {
						pmgr.clear();
						pmgr.getTransaction().begin();
						try {
							a.setState(ActionState.START);
							pmgr.merge(a);
							pmgr.getTransaction().commit();
							exec.run(a);
						} catch (Exception pe) {
							pe.printStackTrace();
						}
					}
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public void init(String clusterId, String nodeId, AtomicReference<NodeState> localNodeState, ActionExecutor exec, ActionCheck config) {
		this.clusterId = clusterId;
		this.nodeId = nodeId;
		this.localNodeState = localNodeState;
		this.exec = exec;
		this.config = config;
	}

}
