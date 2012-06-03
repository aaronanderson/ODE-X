/**
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

import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.ode.runtime.exec.cluster.xml.MessageCheck;

public class MessagePoll implements Runnable {

	@PersistenceContext(unitName = "platform")
	private EntityManager pmgr;

	private String clusterId;
	private String nodeId;
	MessageCheck config;

	private static final Logger log = Logger.getLogger(MessagePoll.class.getName());

	@Override
	public synchronized void run() {
		/*try {// stop for nothing
				// Handle canceled tasks and new tasks
			List<TaskActionImpl> localTasks = null;
			try {
				pmgr.clear();
				Query localTasksQuery = pmgr.createNamedQuery("localTasks");
				localTasksQuery.setParameter("nodeId", nodeId);
				localTasks = (List<TaskActionImpl>) localTasksQuery.getResultList();
			} catch (PersistenceException pe) {
				log.log(Level.SEVERE,"",pe);
			}
			if (localTasks != null) {
				for (TaskActionImpl a : localTasks) {
					if (ActionState.SUBMIT.equals(a.state()) && !exec.getExecutingTasks().containsKey(a.getActionId())) {
						pmgr.clear();
						pmgr.getTransaction().begin();
						try {
							a.setState(ActionState.START);
							pmgr.merge(a);
							pmgr.getTransaction().commit();
							exec.run(a);
						} catch (Exception pe) {
							log.log(Level.SEVERE,"",pe);
						}
					} else if (ActionState.CANCELED.equals(a.state()) && exec.getExecutingTasks().containsKey(a.getActionId())) {
						ActionRunnable ar = exec.getExecutingTasks().get(a.getActionId());
						if (ar != null && !(ar instanceof CancelledActionRunnable)) {
							log.log(Level.FINE, "Cancelling action {0}", a.getActionId());
							ar.cancel();
							if (ActionState.EXECUTING.equals(ar.getAction().state()) || ActionState.FINISH.equals(ar.getAction().state())) {
								log.log(Level.FINER, "Executing cancellation task actionId: {0} old state: {1} new state: {2}", new Object[] {
										ar.getAction().getActionId(), ar.getAction().state(), a.state() });
								exec.run(a);
							} else if (a.start() != null && a.finish() == null) {
								log.log(Level.FINER, "Finalizing task actionId: {0} old state: {1} new state: {2}", new Object[] {
										ar.getAction().getActionId(), ar.getAction().state(), a.state() });
								pmgr.getTransaction().begin();
								try {
									a.setFinish(new Date());
									pmgr.merge(a);
									pmgr.getTransaction().commit();
								} catch (Exception pe) {
									log.log(Level.SEVERE,"",pe);
								}
							}

						}

					}
				}
			}

			// Refresh the executing entries
			for (Iterator<ActionRunnable> i = exec.getExecutingTasks().values().iterator(); i.hasNext();) {
				ActionRunnable runnable = i.next();
				log.log(Level.FINER, "refreshing: ActionId: {0}", runnable.action.getActionId());
				runnable.pollUpdate();
				if (ActionState.CANCELED.equals(runnable.getAction().state()) && runnable.futureTask.isDone() && runnable.getAction().finish() == null) {
					exec.run(runnable.getAction());// put cancelled tasks back on the queue
				}
				//TODO implement task timeouts
			}

		} catch (Throwable t) {
			log.log(Level.SEVERE,"",t);
		}*/
	}

	public void init(String clusterId, String nodeId, MessageCheck config) {
		this.clusterId = clusterId;
		this.nodeId = nodeId;
		this.config = config;
	}

}