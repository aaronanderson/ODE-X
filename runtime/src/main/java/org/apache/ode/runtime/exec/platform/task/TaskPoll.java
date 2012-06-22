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
package org.apache.ode.runtime.exec.platform.task;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.ode.runtime.exec.cluster.xml.ClusterConfig;
import org.apache.ode.runtime.exec.platform.NodeImpl.ClusterId;
import org.apache.ode.runtime.exec.platform.NodeImpl.LocalNodeState;
import org.apache.ode.runtime.exec.platform.NodeImpl.NodeId;
import org.apache.ode.runtime.exec.platform.NodeImpl.TaskCheck;
import org.apache.ode.spi.exec.Node;
import org.apache.ode.spi.exec.Platform.NodeStatus.NodeState;

@Singleton
public class TaskPoll implements Runnable {
	@Inject
	ClusterConfig clusterConfig;

	@ClusterId
	String clusterId;

	@NodeId
	String nodeId;

	@PersistenceContext(unitName = "platform")
	private EntityManager pmgr;

	@TaskCheck
	QueueConnectionFactory queueConFactory;

	private QueueConnection queueConnection;
	private QueueSession taskSession;

	@TaskCheck
	private Queue taskQueue;

	private QueueSender sender;
	private QueueReceiver receiver;

	@Inject
	private TaskExecutor exec;

	@LocalNodeState
	AtomicReference<NodeState> localNodeState;

	org.apache.ode.runtime.exec.cluster.xml.TaskCheck config;

	private static final Logger log = Logger.getLogger(TaskPoll.class.getName());

	@PostConstruct
	public void init() throws Exception {
		this.config = clusterConfig.getTaskCheck();
		queueConnection = queueConFactory.createQueueConnection();
		taskSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
		sender = taskSession.createSender(taskQueue);
		receiver = taskSession.createReceiver(taskQueue, String.format(Node.NODE_MQ_FILTER_NODE, nodeId));
		queueConnection.start();
	}

	@PreDestroy
	public void destroy() throws Exception {
		sender.close();
		receiver.close();
		taskSession.close();
		queueConnection.close();
	}

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

}
