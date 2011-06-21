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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.PersistenceUnit;
import javax.xml.namespace.QName;

import org.apache.ode.runtime.exec.cluster.xml.ActionExecution;
import org.apache.ode.spi.exec.Action;
import org.apache.ode.spi.exec.Action.TaskType;
import org.apache.ode.spi.exec.ActionTask;
import org.apache.ode.spi.exec.ActionTask.ActionContext;
import org.apache.ode.spi.exec.ActionTask.ActionId;
import org.apache.ode.spi.exec.ActionTask.ActionMessage;
import org.apache.ode.spi.exec.ActionTask.ActionMessage.LogType;
import org.apache.ode.spi.exec.ActionTask.ActionState;
import org.apache.ode.spi.exec.ActionTask.ActionStatus;
import org.apache.ode.spi.exec.Component;
import org.apache.ode.spi.exec.MasterActionTask.MasterActionContext;
import org.apache.ode.spi.exec.Platform;
import org.apache.ode.spi.exec.Platform.PlatformAction;
import org.apache.ode.spi.exec.PlatformException;
import org.apache.ode.spi.exec.SlaveActionTask.SlaveActionContext;
import org.w3c.dom.Document;

public class ActionExecutor {

	@PersistenceUnit(unitName = "platform")
	EntityManagerFactory pmgrFactory;

	@Inject
	Provider<InstallMasterAction> installMasterActionProvider;

	@Inject
	Provider<InstallSlaveAction> installSlaveActionProvider;

	Map<QName, ActionEntry> actions = new HashMap<QName, ActionEntry>();

	ConcurrentHashMap<Long, ActionRunnable> executingTasks = new ConcurrentHashMap<Long, ActionRunnable>();

	private String nodeId;
	private ActionExecution config;
	private ThreadPoolExecutor exec;
	
	@PostConstruct
	public void init() {
		System.out.println("Initializing ActionExecutor");
		System.out.println("ActionExecutor Initialized");

	}

	public void online() {
		BlockingQueue<Runnable> actionQueue = new ArrayBlockingQueue<Runnable>(config.getQueueSize());
		RejectedActionExecution rejectionHandler = new RejectedActionExecution();
		exec = new ThreadPoolExecutor(config.getCoreThreads(), config.getMaxThreads(), config.getThreadTimeout(), TimeUnit.SECONDS, actionQueue,
				new ThreadFactory() {

					private final ThreadFactory factory = Executors.defaultThreadFactory();

					@Override
					public Thread newThread(Runnable r) {
						Thread t = factory.newThread(r);
						t.setName("ODE-X Cluster Action Executor");
						t.setDaemon(true);
						return t;
					}
				}, rejectionHandler);
		exec.allowCoreThreadTimeOut(true);
	}

	public void offline() throws PlatformException {
		if (exec != null) {
			exec.shutdown();
			try {
				exec.awaitTermination(config.getShutdownWaitTime(), TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				throw new PlatformException(e);
			}
		}
	}

	public void init(String nodeId, ActionExecution config) {
		this.nodeId = nodeId;
		this.config = config;
	}

	ConcurrentHashMap<Long, ActionRunnable> getExecutingTasks() {
		return executingTasks;
	}

	public void run(org.apache.ode.runtime.exec.platform.Action action) {
		ActionEntry entry = actions.get(action.name());
		ActionTask task = null;
		if (action instanceof SlaveAction) {
			for (ActionEntry slave : entry.slave) {
				if (action.component().equals(slave.component)) {
					task = slave.action.get();
					break;
				}
			}
		} else {
			task = entry.action.get();
		}
		ActionRunnable runnable = new ActionRunnable(action, task);
		FutureTask<ActionRunnable> future = new FutureTask<ActionRunnable>(runnable, runnable);
		runnable.setFutureTask(future);
		exec.submit(future);
		executingTasks.put(action.getActionId(), runnable);
	}

	public TaskType actionType(QName action) throws PlatformException {
		ActionEntry e = actions.get(action);
		if (e == null) {
			throw new PlatformException(String.format("Unsupported action type %s", action));
		}

		if (TaskType.ACTION.equals(e.type) || TaskType.MASTER.equals(e.type)) {
			return e.type;
		} else {
			throw new PlatformException(String.format("Slave action %s requires a master action and may not be directly invoked", action));
		}
	}

	public void setupActions(Set<Component> components) throws PlatformException {
		// add built in actions
		ActionEntry installActionEntry = new ActionEntry(TaskType.MASTER, Platform.PLATFORM, installMasterActionProvider);
		installActionEntry.slave.add(new ActionEntry(TaskType.SLAVE, Platform.PLATFORM, installSlaveActionProvider));
		actions.put(PlatformAction.INSTALL_ACTION.qname(), installActionEntry);
		// add component actions
		if (components != null) {
			for (Component c : components) {
				if (c.actions() != null) {
					for (Action a : c.actions()) {
						ActionEntry e = actions.get(a.getName());
						if (e == null) {
							if (TaskType.ACTION.equals(a.getType()) || TaskType.MASTER.equals(a.getType())) {
								e = new ActionEntry(a.getType(), c.name(), a.getProvider());
								actions.put(a.getName(), e);
							} else {
								throw new PlatformException(String.format("Slave action %s requires existing master", a.getName()));
							}
						} else {
							if ((TaskType.ACTION.equals(e.type) || TaskType.MASTER.equals(e.type))
									&& (TaskType.ACTION.equals(a.getType()) || TaskType.MASTER.equals(a.getType()))) {
								throw new PlatformException(String.format("Action/Master Task already defined for action %s", a.getName()));
							}
							e.slave.add(new ActionEntry(a.getType(), c.name(), a.getProvider()));
						}

					}
				}
			}
		}
	}

	class ActionEntry {
		final TaskType type;
		final QName component;
		final Provider<? extends ActionTask> action;

		ActionEntry(TaskType type, QName component, Provider<? extends ActionTask> action) {
			this.type = type;
			this.component = component;
			this.action = action;
		}

		List<ActionEntry> slave = new ArrayList<ActionEntry>();
	}

	public ActionId executeAction(QName action, Document actionInput, String target) throws PlatformException {
		ActionEntry ae = actions.get(action);
		EntityManager pmgr = pmgrFactory.createEntityManager();
		pmgr.getTransaction().begin();
		try {
			org.apache.ode.runtime.exec.platform.Action a = new org.apache.ode.runtime.exec.platform.Action();
			a.setActionType(action);
			a.setComponent(ae.component);
			a.setNodeId(target);
			a.setInput(actionInput);
			if (nodeId.equals(target)) {
				a.setState(ActionState.START);
			} else {
				a.setState(ActionState.SUBMIT);
			}
			pmgr.persist(a);
			pmgr.getTransaction().commit();
			if (nodeId.equals(target)) {
				run(a);
			}
			return a.id();
		} catch (PersistenceException pe) {
			pmgr.getTransaction().rollback();
			throw new PlatformException(pe);
		} finally {
			pmgr.close();
		}

	}

	public ActionId executeMasterAction(QName action, Document actionInput, Set<String> targets) throws PlatformException {
		ActionEntry ae = actions.get(action);
		String masterTarget = null;
		Set<org.apache.ode.runtime.exec.platform.Action> localActions = new HashSet<org.apache.ode.runtime.exec.platform.Action>();
		if (targets.contains(nodeId)) {
			masterTarget = nodeId;
		} else {
			masterTarget = targets.iterator().next();
		}

		EntityManager pmgr = pmgrFactory.createEntityManager();
		try {
			pmgr.getTransaction().begin();
			try {
				MasterAction m = new MasterAction();
				m.setActionType(action);
				m.setComponent(ae.component);
				m.setNodeId(masterTarget);
				m.setInput(actionInput);
				if (nodeId.equals(masterTarget)) {
					m.setState(ActionState.START);
					localActions.add(m);
				} else {
					m.setState(ActionState.SUBMIT);
				}
				pmgr.persist(m);
				for (ActionEntry sae : ae.slave) {
					for (String node : targets) {
						SlaveAction s = new SlaveAction();
						s.setActionType(action);
						s.setComponent(sae.component);
						s.setInput(actionInput);
						if (TaskType.SINGLE_SLAVE.equals(sae.type)) {
							node = masterTarget;
						}
						s.setNodeId(node);
						if (nodeId.equals(node)) {
							s.setState(ActionState.START);
							localActions.add(s);
						} else {
							s.setState(ActionState.SUBMIT);
						}
						pmgr.persist(s);
						if (TaskType.SINGLE_SLAVE.equals(sae.type)) {
							break;
						}
					}
				}

				pmgr.getTransaction().commit();
				for (org.apache.ode.runtime.exec.platform.Action la : localActions) {
					run(la);
				}
				return m.id();
			} catch (PersistenceException pe) {
				pmgr.getTransaction().rollback();
				throw new PlatformException(pe);
			}
		} finally {
			pmgr.close();
		}

	}

	public ActionStatus status(ActionIdImpl actionId) throws PlatformException {

		EntityManager pmgr = pmgrFactory.createEntityManager();
		pmgr.getTransaction().begin();
		try {
			return pmgr.find(org.apache.ode.runtime.exec.platform.Action.class, actionId.id);
		} catch (PersistenceException pe) {
			pmgr.getTransaction().rollback();
			throw new PlatformException(pe);
		} finally {
			pmgr.close();
		}

	}

	public void cancel(ActionIdImpl actionId) throws PlatformException {

		EntityManager pmgr = pmgrFactory.createEntityManager();
		pmgr.getTransaction().begin();
		try {
			org.apache.ode.runtime.exec.platform.Action action = pmgr.find(org.apache.ode.runtime.exec.platform.Action.class, actionId.id);
			// TODO update status to cancel. For Master action iterate through
			// slaves and cancel as well. If local task cancel it and remove it.
		} catch (PersistenceException pe) {
			pmgr.getTransaction().rollback();
			throw new PlatformException(pe);
		} finally {
			pmgr.close();
		}

	}

	public class RejectedActionExecution implements RejectedExecutionHandler {
		@Override
		public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
			System.out.println(runnable.toString() + " : I've been rejected ! ");
		}
	}

	public class ActionRunnable implements Runnable {

		ActionTask task;
		ActionContextImpl context;
		org.apache.ode.runtime.exec.platform.Action action;
		FutureTask<ActionRunnable> futureTask;
		CountDownLatch refreshLatch = new CountDownLatch(1);
		Lock updateLock = new ReentrantLock();
		EntityManager pmgr = pmgrFactory.createEntityManager();

		public ActionRunnable(org.apache.ode.runtime.exec.platform.Action action, ActionTask task) {
			this.task = task;
			this.action = action;
			this.context = new ActionContextImpl(this);
		}

		public ActionRunnable(MasterAction action, ActionTask task) {
			this.task = task;
			this.action = action;
			this.context = new ActionContextImpl(this);
		}

		public ActionRunnable(SlaveAction action, ActionTask task) {
			this.task = task;
			this.action = action;
			this.context = new ActionContextImpl(this);
		}

		public org.apache.ode.runtime.exec.platform.Action getAction() {
			return action;
		}

		CountDownLatch getRefreshLatch() {
			return refreshLatch;
		}

		public void setFutureTask(FutureTask<ActionRunnable> future) {
			this.futureTask = future;
		}

		public void save() {
			updateLock.lock();
			try {
				pmgr.getTransaction().begin();
				try {
					pmgr.persist(action);
					pmgr.getTransaction().commit();
				} catch (PersistenceException pe) {
					pe.printStackTrace();
					pmgr.getTransaction().rollback();
				}
			} finally {
				updateLock.unlock();
			}
		}

		public void contextUpdate() throws PlatformException {
			updateLock.lock();
			try {
				pmgr.getTransaction().begin();
				try {
					pmgr.persist(action);
					pmgr.getTransaction().commit();
				} catch (PersistenceException pe) {
					pmgr.getTransaction().rollback();
					throw new PlatformException(pe);
				}
			} finally {
				updateLock.unlock();
			}
		}

		public void refresh() {
			updateLock.lock();
			try {
				pmgr.getTransaction().begin();
				try {
					pmgr.refresh(action);
					pmgr.getTransaction().commit();
				} catch (PersistenceException pe) {
					pe.printStackTrace();
					pmgr.getTransaction().rollback();
				}
			} finally {
				updateLock.unlock();
			}
		}

		@Override
		public void run() {
			try {
				action.setStart(new Date());
				task.start(context);
				if (ActionState.START.equals(action.state())) {
					action.setState(ActionState.EXECUTING);
					save();
				} else {
					action.setFinish(new Date());
					save();
					remove();
					return;
				}
			} catch (PlatformException pe) {
				context.log(new ActionMessage(new Date(), LogType.ERROR, pe.getMessage()));
				action.setState(ActionState.FAILED);
				action.setFinish(new Date());
				save();
				remove();
				return;
			}

			try {
				task.run(context);
				if (ActionState.EXECUTING.equals(action.state())) {
					action.setState(ActionState.FINISH);
				} // finish should always run if started completed
			} catch (PlatformException pe) {
				context.log(new ActionMessage(new Date(), LogType.ERROR, pe.getMessage()));
				action.setState(ActionState.FAILED);
			} finally {
				save();
			}

			try {
				task.finish(context);
				if (ActionState.FINISH.equals(action.state())) {
					action.setState(ActionState.COMPLETED);
				}
			} catch (PlatformException pe) {
				context.log(new ActionMessage(new Date(), LogType.ERROR, pe.getMessage()));
				action.setState(ActionState.FAILED);
			} finally {
				action.setFinish(new Date());
				save();
			}
			remove();

		}

		public void remove() {
			executingTasks.remove(action.getActionId());
			pmgr.close();
		}

		public void pollUpdate() {
			refresh();
			if (!futureTask.isDone() && ActionState.CANCELED.equals(action.state())) {
				futureTask.cancel(true);
				try {
					action.setState(ActionState.CANCELED);
					task.finish(context);
				} catch (PlatformException pe) {
					context.log(new ActionMessage(new Date(), LogType.ERROR, pe.getMessage()));
					action.setState(ActionState.FAILED);
				} finally {
					save();
				}
				remove();
			}
			refreshLatch.countDown();
		}

	}

	public class ActionContextImpl implements ActionContext {

		protected ActionRunnable runnable;
		private CountDownLatch refreshLatch;

		public ActionContextImpl(ActionRunnable runnable) {
			this.runnable = runnable;
		}

		@Override
		public ActionId id() {
			return runnable.getAction().id();
		}

		@Override
		public QName name() {
			return runnable.getAction().name();
		}

		@Override
		public void log(ActionMessage message) {
			runnable.getAction().messages().add(message);
		}

		@Override
		public Document input() {
			return runnable.getAction().getInput();
		}

		@Override
		public void refresh() {
			try {
				refreshLatch.await();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public ActionState getStatus() {
			return runnable.getAction().state();
		}

		@Override
		public void updateStatus(ActionState state) {
			runnable.getAction().setState(state);
		}

		@Override
		public void updateResult(Document result) {
			try {
				runnable.getAction().setResult(result);
			} catch (PlatformException e) {
				e.printStackTrace();
			}
		}

	}

	public class MasterActionContextImpl extends ActionContextImpl implements MasterActionContext {

		public MasterActionContextImpl(ActionRunnable runnable) {
			super(runnable);
		}

		@Override
		public Set<ActionStatus> slaveStatus() {
			MasterAction ma = (MasterAction) runnable.getAction();
			return null;
		}

		@Override
		public void setInput(String nodeId, Document input) throws PlatformException {
			MasterAction ma = (MasterAction) runnable.getAction();
			for (SlaveAction a : ma.slaves) {
				if (a.getNodeId().equals(nodeId)) {
					a.setInput(input);
					runnable.contextUpdate();
					return;
				}
			}
			throw new PlatformException(String.format("Unknown nodeId %s", nodeId));

		}

	}

	public class SlaveActionContextImpl extends ActionContextImpl implements SlaveActionContext {

		public SlaveActionContextImpl(ActionRunnable runnable) {
			super(runnable);
		}

	}

}
