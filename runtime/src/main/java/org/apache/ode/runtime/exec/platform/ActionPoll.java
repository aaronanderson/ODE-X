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

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.apache.ode.runtime.exec.cluster.xml.ActionCheck;
import org.apache.ode.runtime.exec.platform.ActionExecutor.ActionRunnable;
import org.apache.ode.runtime.exec.platform.ActionExecutor.CancelledActionRunnable;
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
				// Handle canceled tasks and new tasks
			List<Action> localTasks = null;
			try {
				pmgr.clear();
				Query localTasksQuery = pmgr.createNamedQuery("localTasks");
				localTasksQuery.setParameter("nodeId", nodeId);
				localTasks = (List<Action>) localTasksQuery.getResultList();
			} catch (PersistenceException pe) {
				pe.printStackTrace();
			}
			if (localTasks != null) {
				for (Action a : localTasks) {
					if (ActionState.SUBMIT.equals(a.state()) && !exec.getExecutingTasks().containsKey(a.getActionId())) {
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
					} else if (ActionState.CANCELED.equals(a.state()) && exec.getExecutingTasks().containsKey(a.getActionId())) {
						ActionRunnable ar = exec.getExecutingTasks().get(a.getActionId());
						if (ar != null && !(ar instanceof CancelledActionRunnable)) {
							//System.out.format("Cancelling action %s\n",a.getActionId());
							ar.cancel();
							if (ActionState.EXECUTING.equals(ar.getAction().state()) || ActionState.FINISH.equals(ar.getAction().state())) {
								//System.out.format("Executing cancellation task actionId: %s old state: %s new state: %s\n",ar.getAction().getActionId(),ar.getAction().state(), a.state());
								exec.run(a);
							} else if (a.start() != null && a.finish() == null) {
								//System.out.format("Finalizing task actionId: %s old state: %s new state: %s\n",ar.getAction().getActionId(),ar.getAction().state(), a.state());
								pmgr.getTransaction().begin();
								try {
									a.setFinish(new Date());
									pmgr.merge(a);
									pmgr.getTransaction().commit();
								} catch (Exception pe) {
									pe.printStackTrace();
								}
							}

						}

					}
				}
			}

			// Refresh the executing entries
			for (Iterator<ActionRunnable> i = exec.getExecutingTasks().values().iterator(); i.hasNext();) {
				ActionRunnable runnable = i.next();
				// System.out.format("refreshing: ActionId: %s\n", runnable.action.getActionId());
				runnable.pollUpdate();
				if (ActionState.CANCELED.equals(runnable.getAction().state()) && runnable.futureTask.isDone() && runnable.getAction().finish() == null) {
					exec.run(runnable.getAction());// put cancelled tasks back on the queue
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
