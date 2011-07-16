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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;
import javax.persistence.PersistenceUnit;
import javax.persistence.RollbackException;
import javax.xml.namespace.QName;

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
import org.apache.ode.spi.exec.Executors;
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

	@Inject
	Executors executors;

	Map<QName, ActionEntry> actions = new HashMap<QName, ActionEntry>();

	ConcurrentHashMap<Long, ActionRunnable> executingTasks = new ConcurrentHashMap<Long, ActionRunnable>();

	private String nodeId;
	private ExecutorService exec;

	private static final Logger log = Logger.getLogger(ActionExecutor.class.getName());

	@PostConstruct
	public void init() {
		log.fine("Initializing ActionExecutor");
		log.fine("ActionExecutor Initialized");

	}

	public void online() throws PlatformException {
		exec = executors.onlineClusterActionExecutor(new RejectedActionExecution());
	}

	public void offline() throws PlatformException {
		executors.offlineClusterActionExecutor();
	}

	public void init(String nodeId) {
		this.nodeId = nodeId;
	}

	ConcurrentHashMap<Long, ActionRunnable> getExecutingTasks() {
		return executingTasks;
	}

	public void run(org.apache.ode.runtime.exec.platform.Action action) throws PlatformException {
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
		ActionRunnable runnable = null;
		if (ActionState.CANCELED.equals(action.state())) {
			runnable = new CancelledActionRunnable(action.getActionId(), task);
		} else {
			runnable = new ActionRunnable(action.getActionId(), task);
		}
		FutureTask<ActionRunnable> future = new FutureTask<ActionRunnable>(runnable, runnable);
		runnable.setFutureTask(future);
		exec.submit(future);
		executingTasks.put(action.getActionId(), runnable);
	}

	TaskType actionType(QName action) throws PlatformException {
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

	void setupActions(Set<Component> components) throws PlatformException {
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

	ActionId executeAction(QName action, Document actionInput, String target) throws PlatformException {
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
			throw new PlatformException(pe);
		} finally {
			pmgr.close();
		}

	}

	ActionId executeMasterAction(QName action, Document actionInput, Set<String> targets) throws PlatformException {
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
						s.setMaster(m);
						m.addSlave(s);
						if (TaskType.SINGLE_SLAVE.equals(sae.type)) {
							break;
						}
					}
				}
				pmgr.persist(m);
				pmgr.getTransaction().commit();
				for (org.apache.ode.runtime.exec.platform.Action la : localActions) {
					run(la);
				}
				return m.id();
			} catch (PersistenceException pe) {
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
			ActionStatus status = pmgr.find(org.apache.ode.runtime.exec.platform.Action.class, actionId.id);
			pmgr.detach(status);
			return status;
		} catch (PersistenceException pe) {
			throw new PlatformException(pe);
		} finally {
			pmgr.close();
		}

	}

	public void cancel(ActionIdImpl actionId) throws PlatformException {

		EntityManager pmgr = pmgrFactory.createEntityManager();
		try {
			while (true) {
				pmgr.clear();
				pmgr.getTransaction().begin();
				try {
					org.apache.ode.runtime.exec.platform.Action action = pmgr.find(org.apache.ode.runtime.exec.platform.Action.class, actionId.id);
					if (ActionState.START.equals(action.state()) || ActionState.EXECUTING.equals(action.state()) || ActionState.FINISH.equals(action.state())) {
						action.setState(ActionState.CANCELED);
						pmgr.merge(action);
						pmgr.getTransaction().commit();
						if (action instanceof MasterAction) {
							for (SlaveAction s : ((MasterAction) action).getSlaves()) {
								cancel((ActionIdImpl) s.id());
							}
						}
					} else {
						pmgr.getTransaction().rollback();
					}
					break;
				} catch (PersistenceException pe) {
					if (pe instanceof RollbackException && pe.getCause() instanceof OptimisticLockException) {
						OptimisticLockException oe = ((OptimisticLockException) pe.getCause());
						log.log(Level.WARNING, "", oe);
					} else {
						log.log(Level.SEVERE, "", pe);
						break;
					}
				}
			}
		} finally {
			pmgr.close();
		}
	}

	private class RejectedActionExecution implements RejectedExecutionHandler {
		@Override
		public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
			ActionRunnable ar = (ActionRunnable) runnable;
			log.log(Level.SEVERE, "ActionTask Rejected {0}", ar.getAction().getActionId());
			ar.getAction().setState(ActionState.CANCELED);
			ar.save();
		}
	}

	public class ActionRunnable implements Runnable {

		ActionTask task;
		ActionContextImpl context;
		org.apache.ode.runtime.exec.platform.Action action;
		FutureTask<ActionRunnable> futureTask;
		CyclicBarrier refreshBarrier = new CyclicBarrier(2);
		Lock updateLock = new ReentrantLock();
		EntityManager pmgr = pmgrFactory.createEntityManager();
		volatile boolean canceled = false;

		public ActionRunnable(long actionId, ActionTask task) throws PlatformException {
			try {
				action = pmgr.find(org.apache.ode.runtime.exec.platform.Action.class, actionId);
			} catch (PersistenceException pe) {
				throw new PlatformException(pe);
			}
			if (action instanceof MasterAction) {
				this.context = new MasterActionContextImpl(this);
			} else if (action instanceof SlaveAction) {
				this.context = new SlaveActionContextImpl(this);
			} else {
				this.context = new ActionContextImpl(this);
			}

			this.task = task;
		}

		public org.apache.ode.runtime.exec.platform.Action getAction() {
			return action;
		}

		CyclicBarrier getRefreshBarrier() {
			return refreshBarrier;
		}

		public void setFutureTask(FutureTask<ActionRunnable> future) {
			this.futureTask = future;
		}

		public void cancel() {
			canceled = true;
			futureTask.cancel(true);
			remove();
		}

		public boolean save() {
			if (!canceled) {
				updateLock.lock();
				try {
					pmgr.getTransaction().begin();
					try {
						pmgr.merge(action);
						pmgr.getTransaction().commit();
						return true;
					} catch (PersistenceException pe) {
						if (pe instanceof RollbackException && pe.getCause() instanceof OptimisticLockException) {
							context.refresh();
						} else {
							log.log(Level.SEVERE, "", pe);
						}
						return false;
					}
				} finally {
					updateLock.unlock();
				}
			}
			return false;
		}

		public boolean contextUpdate(org.apache.ode.runtime.exec.platform.Action action) throws PlatformException {
			if (!canceled) {
				updateLock.lock();
				try {
					pmgr.getTransaction().begin();
					try {
						pmgr.merge(action);
						pmgr.getTransaction().commit();
						return true;
					} catch (PersistenceException pe) {
						if (pe instanceof RollbackException && pe.getCause() instanceof OptimisticLockException) {
							context.refresh();
						}
						throw new PlatformException(pe);
					}
				} finally {
					updateLock.unlock();
				}
			}
			return false;
		}

		/*
		 * Concurrency Model: Once an action has been persisted with the action state START only the node that is executing the task will be updating the
		 * persisted state except for the following conditions:
		 * 
		 * cancel: At any time any node may update the action state to the cancelled status. Typically this will generate an optimistic lock exception that
		 * should be handled gracefully. On each of the save methods if an optimistic lock is caught the refresh lock is acquired and the polling thread will
		 * will refresh the state. If it is cancelled, the polling thread will use the futuretask to cancel the action. If the action was in an executing state it will 
		 * be run again as a CancelledActionRunnable so that the task's finish method can be invoked. This allows cancelled tasks the opportunity to perform cleanup outside the polling thread.   
		 * If the action was completed or in a start state the polling thread will simply set the finish date and clean up the thread.
		 * 
		 * timeout: similar to cancel. The polling thread will examine the last modified timestamp and if the task is in a running state and the timeout threshold exceed the
		 * task will be cancelled. 
		 * 
		 * refresh: A running task may block on the refreshBarrier and wait for the action polling thread to update the action state. This is typically useful in a
		 * master/slave communication pattern
		 * 
		 * master to slave: When slaves start up they wait for the master to finishing starting first before they do. This allows the master to update the slave
		 * action input at any time during the start and executing phase. When slaves update their result based on the current input it provides a master/slave
		 * communication channel. It is anticipated that both the master and slave will use the refresh mechanism to retrieve input/result updates while in an
		 * executing state.
		 */

		@Override
		public void run() {
			if (action instanceof SlaveAction) {// Master should start before slave
				SlaveAction saction = (SlaveAction) action;
				while (ActionState.START.equals(saction.masterStatus().state())) {
					context.refresh();
				}
				if (!ActionState.EXECUTING.equals(saction.masterStatus().state())) {
					saction.setState(ActionState.CANCELED);
					action.setFinish(new Date());
					save();
					remove();
					return;
				}
			}

			try {
				Date start = new Date();
				action.setStart(start);
				save();
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
			} catch (Throwable pe) {
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
			} catch (Throwable pe) {
				context.log(new ActionMessage(new Date(), LogType.ERROR, pe.getMessage()));
				action.setState(ActionState.FAILED);
			} finally {
				save();
			}

			if (action instanceof MasterAction) {// Finish executing slaves
				MasterAction maction = (MasterAction) action;
				boolean finished = false;
				do {
					finished = true;
					for (SlaveAction slave : maction.getSlaves()) {
						switch (slave.state()) {
						case SUBMIT:
						case START:
						case EXECUTING:
							finished = false;
							try {
								context.refresh();
							} catch (Throwable pe) {
								context.log(new ActionMessage(new Date(), LogType.ERROR, pe.getMessage()));
								action.setState(ActionState.FAILED);
								break;
							}
						}
					}
				} while (!finished);
			}

			try {
				task.finish(context);
				if (ActionState.FINISH.equals(action.state())) {
					action.setState(ActionState.COMPLETED);
				}
			} catch (Throwable pe) {
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
			if (refreshBarrier.getNumberWaiting() == 1 && updateLock.tryLock()) {
				try {
					// HashMap refreshProperties = new HashMap();
					// refreshProperties.put("javax.persistence.cache.retrieveMode", CacheRetrieveMode.BYPASS);
					pmgr.refresh(action);
					if (action instanceof SlaveAction) { // not sure why but can't get master to refresh even with cascade.refresh
						pmgr.refresh(((SlaveAction) action).master);
					}
				} catch (PersistenceException pe) {
					log.log(Level.SEVERE, "", pe);

				} finally {
					updateLock.unlock();
					try {
						refreshBarrier.await();
					} catch (Exception e) {
						log.log(Level.FINE, "", e);
					}
				}

			}
		}

	}

	public class CancelledActionRunnable extends ActionRunnable {
		public CancelledActionRunnable(long actionId, ActionTask task) throws PlatformException {
			super(actionId, task);
		}

		@Override
		public void run() {
			// finish should be called on all cancelled tasks if the run method was invoked
			if (ActionState.CANCELED.equals(action.state())) {// This task was thrown back on the queue after being cancelled
				try {
					task.finish(context);
				} catch (Throwable pe) {
					context.log(new ActionMessage(new Date(), LogType.ERROR, pe.getMessage()));
					action.setState(ActionState.FAILED);
				} finally {
					action.setFinish(new Date());
					save();
					remove();
				}
				return;
			}

		}

	}

	public class ActionContextImpl implements ActionContext {

		protected ActionRunnable runnable;

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
			try {
				do {
					List<ActionMessage> messages = runnable.getAction().getMessages();
					messages.add(message);
					runnable.getAction().setMessages(messages);
				} while (!runnable.contextUpdate(runnable.getAction()));
			} catch (PlatformException e) {
				log.log(Level.SEVERE, "", e);
			}
		}

		@Override
		public Document input() {
			return runnable.getAction().getInput();
		}

		@Override
		public void refresh() {
			log.log(Level.FINER, "Refresh: ActionId: {0}", runnable.getAction().getActionId());
			try {
				runnable.getRefreshBarrier().await();
			} catch (Exception e) {
				log.log(Level.FINE, "", e);
			}
		}

		@Override
		public ActionState getStatus() {
			return runnable.getAction().state();
		}

		@Override
		public void updateStatus(ActionState state) throws PlatformException {
			if (ActionState.CANCELED.equals(state)) {
				throw new PlatformException("ActionTasks may not cancel themselves");
			}
			if (!(ActionState.PARTIAL_FAILURE.equals(state) || ActionState.FAILED.equals(state))) {
				throw new PlatformException("ActionTasks may only specify the failed and partial failure states. All others are managed by the platforms ");
			}
			do {
				runnable.getAction().setState(state);
			} while (!runnable.contextUpdate(runnable.getAction()));
		}

		@Override
		public void updateResult(Document result) throws PlatformException {
			do {
				runnable.getAction().setResult(result);
			} while (!runnable.contextUpdate(runnable.getAction()));
		}

	}

	public class MasterActionContextImpl extends ActionContextImpl implements MasterActionContext {

		public MasterActionContextImpl(ActionRunnable runnable) {
			super(runnable);
		}

		@Override
		public Set<ActionStatus> slaveStatus() {
			MasterAction ma = (MasterAction) runnable.getAction();
			return ma.slaveStatus();
		}

		@Override
		public void updateInput(String nodeId, Document input) throws PlatformException {
			MasterAction ma = (MasterAction) runnable.getAction();
			for (SlaveAction a : ma.slaves) {
				if (a.nodeId().equals(nodeId)) {
					if (ActionState.START.equals(runnable.getAction().state())) {
						if (ActionState.START.equals(a.state())) {
							do {
								a.setInput(input);
							} while (!runnable.contextUpdate(a));
						} else {
							throw new PlatformException(String.format("Invalid state. Master: %s Slave %s", runnable.getAction().state(), a.state()));
						}
					} else if (ActionState.EXECUTING.equals(runnable.getAction().state())) {
						while (ActionState.START.equals(a.state())) {
							refresh();
						}
						if (ActionState.EXECUTING.equals(a.state())) {
							do {
								a.setInput(input);
							} while (!runnable.contextUpdate(a));
						} else {
							throw new PlatformException(String.format("Invalid state. Master: %s Slave %s", runnable.getAction().state(), a.state()));
						}
					} else {
						throw new PlatformException(String.format("Invalid state. Master: %s Slave %s", runnable.getAction().state(), a.state()));
					}

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

		@Override
		public ActionStatus masterStatus() {
			SlaveAction sa = (SlaveAction) runnable.getAction();
			return sa.masterStatus();
		}

	}

}
