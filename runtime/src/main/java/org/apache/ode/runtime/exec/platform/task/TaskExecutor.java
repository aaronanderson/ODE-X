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

import static org.apache.ode.runtime.exec.platform.NodeImpl.PLATFORM_JAXB_CTX;
import static org.apache.ode.spi.exec.Node.NODE_MQ_CORRELATIONID_ACTION;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.PersistenceUnit;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.ode.runtime.exec.cluster.xml.ClusterConfig;
import org.apache.ode.spi.exec.platform.xml.ExchangeType;
import org.apache.ode.runtime.exec.platform.NodeImpl.LocalNodeState;
import org.apache.ode.runtime.exec.platform.NodeImpl.NodeId;
import org.apache.ode.runtime.exec.platform.NodeImpl.TaskCheck;
import org.apache.ode.runtime.exec.platform.target.TargetAllImpl;
import org.apache.ode.runtime.exec.platform.target.TargetClusterImpl;
import org.apache.ode.runtime.exec.platform.target.TargetImpl.TargetPK;
import org.apache.ode.runtime.exec.platform.target.TargetNodeImpl;
import org.apache.ode.runtime.exec.platform.task.TaskActionImpl.TaskActionIdImpl;
import org.apache.ode.runtime.exec.platform.task.TaskCallable.TaskResult;
import org.apache.ode.runtime.exec.platform.task.TaskImpl.TaskIdImpl;
import org.apache.ode.spi.exec.Component;
import org.apache.ode.spi.exec.Executors;
import org.apache.ode.spi.exec.Message.LogLevel;
import org.apache.ode.spi.exec.Node;
import org.apache.ode.spi.exec.Platform.NodeStatus.NodeState;
import org.apache.ode.spi.exec.PlatformException;
import org.apache.ode.spi.exec.target.Target;
import org.apache.ode.spi.exec.task.CoordinatedTaskActionCoordinator;
import org.apache.ode.spi.exec.task.Task.TaskState;
import org.apache.ode.spi.exec.task.TaskAction;
import org.apache.ode.spi.exec.task.TaskActionCoordinator;
import org.apache.ode.spi.exec.task.TaskActionDefinition;
import org.apache.ode.spi.exec.task.TaskCallback;
import org.apache.ode.spi.exec.task.TaskDefinition;
import org.apache.ode.spi.exec.task.TaskException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Singleton
public class TaskExecutor implements Runnable {

	@PersistenceUnit(unitName = "platform")
	EntityManagerFactory pmgrFactory;

	@Inject
	Executors executors;

	@Inject
	Provider<TaskCallable> taskProvider;

	@Inject
	Provider<TaskActionCallable> actionProvider;

	private ExecutorService exec;

	private static final Logger log = Logger.getLogger(TaskExecutor.class.getName());

	@Inject
	ClusterConfig clusterConfig;

	@NodeId
	String nodeId;

	@TaskCheck
	QueueConnectionFactory queueConFactory;

	private QueueConnection pollQueueConnection;
	private QueueSession pollTaskSession;

	@TaskCheck
	private Queue taskQueue;

	private QueueReceiver taskQueueReceiver;

	private Node node;

	@LocalNodeState
	AtomicReference<NodeState> localNodeState;

	org.apache.ode.runtime.exec.cluster.xml.TaskCheck config;

	ConcurrentHashMap<String, TaskActionCallable> executingActions = new ConcurrentHashMap<String, TaskActionCallable>();

	@PostConstruct
	public void init() {
		log.fine("Initializing ActionExecutor");
		log.fine("ActionExecutor Initialized");
		this.config = clusterConfig.getTaskCheck();
		try {
			pollQueueConnection = queueConFactory.createQueueConnection();
			pollTaskSession = pollQueueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			taskQueueReceiver = pollTaskSession.createReceiver(taskQueue, String.format(Node.NODE_MQ_FILTER_TASK_AND_TASK_ACTION, nodeId));
			pollQueueConnection.start();
		} catch (JMSException e) {
			log.log(Level.SEVERE, "", e);
		}
	}

	@PreDestroy
	public void destroy() {
		try {
			pollQueueConnection.close();
		} catch (JMSException e) { //don't care about JMS errors on closure
		}
	}

	@Override
	public synchronized void run() {
		//TODO send requestor failure message if task is not even started below
		try {
			while (true) {
				try {
					BytesMessage message = (BytesMessage) taskQueueReceiver.receive(config.getFrequency());
					if (message == null) {
						break;
					}
					Queue requestor = (Queue) message.getJMSReplyTo();
					String correlationId = message.getJMSCorrelationID();
					byte[] payload = new byte[(int) message.getBodyLength()];
					message.readBytes(payload);
					Unmarshaller umarshaller = PLATFORM_JAXB_CTX.createUnmarshaller();
					if (message.getStringProperty(Node.NODE_MQ_PROP_TASKID) != null) {
						//TODO tasks are typically submitted from platform, but in future would like to support task initiation externally from MQ and provide updates like actions
						JAXBElement<org.apache.ode.spi.exec.platform.xml.Task> element = umarshaller.unmarshal(new StreamSource(new ByteArrayInputStream(
								payload)), org.apache.ode.spi.exec.platform.xml.Task.class);
						org.apache.ode.spi.exec.platform.xml.Task xmlTask = element.getValue();
						if (org.apache.ode.spi.exec.platform.xml.TaskState.SUBMIT == xmlTask.getState()) {

							submitTask(LogLevel.valueOf(xmlTask.getLogLevel().value()), correlationId, requestor, xmlTask.getName(),
									xmlTask.getExchange() != null ? xmlTask.getExchange().getPayload() : null, null, ExchangeTypes(xmlTask));
						} else {
							log.log(Level.WARNING, String.format("Unsupported Task state %s", xmlTask.getState()));
						}
					} else if (message.getStringProperty(Node.NODE_MQ_PROP_ACTIONID) != null) {
						JAXBElement<org.apache.ode.spi.exec.platform.xml.TaskAction> element = umarshaller.unmarshal(new StreamSource(new ByteArrayInputStream(
								payload)), org.apache.ode.spi.exec.platform.xml.TaskAction.class);
						org.apache.ode.spi.exec.platform.xml.TaskAction taskAction = element.getValue();
						if (org.apache.ode.spi.exec.platform.xml.TaskActionState.SUBMIT == taskAction.getState()) {
							if (correlationId == null) {
								String.format(NODE_MQ_CORRELATIONID_ACTION, System.currentTimeMillis());
							}
							submitTaskAction(LogLevel.valueOf(taskAction.getLogLevel().value()), correlationId, requestor, taskAction.getTaskId(),
									taskAction.getName(), taskAction.getExchange() != null ? taskAction.getExchange().getPayload() : null);
						} else if (org.apache.ode.spi.exec.platform.xml.TaskActionState.EXECUTE == taskAction.getState()
								|| org.apache.ode.spi.exec.platform.xml.TaskActionState.COMMIT == taskAction.getState()
								|| org.apache.ode.spi.exec.platform.xml.TaskActionState.ROLLBACK == taskAction.getState()) {
							TaskActionCallable callable = executingActions.get(correlationId);
							if (callable != null) {
								callable.externalUpdate(taskAction);
							} else {
								log.log(Level.WARNING,
										String.format("TaskAction %s correlationId %s not found in running state", taskAction.getActionId(), correlationId));
							}

						} else {
							log.log(Level.WARNING, String.format("Unsupported TaskAction state %s", taskAction.getState()));
						}
					}/* else {
						StringBuilder sb = new StringBuilder();
						for (Enumeration e = message.getPropertyNames(); e.hasMoreElements(); sb.append(e.nextElement()).append(", "))
							;
						log.log(Level.WARNING, String.format("Received request does not contain necessary type headers %s", sb));
						}*/

				} catch (JMSException e) {
					if (e.getCause() instanceof InterruptedException) {
						break;
					} else {
						log.log(Level.SEVERE, "", e);
					}
				}
			}

		} catch (Throwable t) {
			log.log(Level.SEVERE, "", t);
		}

	}

	public void configure(Node node) {
		this.node = node;
	}

	public void online() throws PlatformException {
		exec = executors.initClusterTaskExecutor(new RejectedTaskExecution());
	}

	public void offline() throws PlatformException {
		executors.destroyClusterTaskExecutor();
	}

	private class RejectedTaskExecution implements RejectedExecutionHandler {
		@Override
		public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
			if (runnable instanceof TaskCallable) {
				//log.log(Level.SEVERE, "ActionTask Rejected {0}", ar.getAction().getActionId());	
			} else if (runnable instanceof TaskActionCallable) {
				//log.log(Level.SEVERE, "ActionTask Rejected {0}", ar.getAction().getActionId());
			}
		}
	}

	public class TaskInfo {
		public TaskIdImpl id;
		public Future<TaskResult> result;
	}

	public TaskInfo submitTask(LogLevel logLevel, String correlationId, Queue requestor, QName task, Element taskInput, TaskCallback<?, ?> callback,
			Target... targets) throws TaskException {
		//TaskCoordinators run on node where task is submitted. Also all DB 
		TaskDefinition def = node.getTaskDefinitions().get(task);
		if (def == null) {
			throw new TaskException(String.format("Unsupported Task %s", task.toString()));
		}
		EntityManager pmgr = pmgrFactory.createEntityManager();
		pmgr.getTransaction().begin();
		try {
			org.apache.ode.runtime.exec.platform.task.TaskImpl t = new org.apache.ode.runtime.exec.platform.task.TaskImpl();
			t.setName(task);
			/*defer
			t.setInput(taskInput); 
			Set<TargetImpl> targetEntities = new HashSet<TargetImpl>();
			for (Target target : targets) {
				targetEntities.add((TargetImpl) target);
			}
			t.setTargets(targetEntities);
			//t.setComponent(ae.component);
			*/t.setNodeId(nodeId);
			t.setState(TaskState.SUBMIT);
			pmgr.persist(t);
			pmgr.getTransaction().commit();
			TaskInfo info = new TaskInfo();
			info.id = (TaskIdImpl) t.id();
			TaskCallable c = taskProvider.get();
			c.config(node, config, logLevel, correlationId, requestor, info.id, taskInput, callback, targets);
			info.result = exec.submit(c);
			return info;
		} catch (PersistenceException pe) {
			throw new TaskException(pe);
		} finally {
			pmgr.close();
		}

	}

	public class TaskActionInfo {
		public TaskActionIdImpl id;
		public Future<TaskAction.TaskActionState> state;
	}

	public TaskActionInfo submitTaskAction(LogLevel logLevel, String correlationId, Queue requestor, String taskId, QName action, Element taskInput)
			throws TaskException {
		//TaskCoordinators run on node where task is submitted. Also all DB 
		TaskActionDefinition def = node.getTaskActionDefinitions().get(action);
		if (def == null) {
			throw new TaskException(String.format("Unsupported TaskAction %s", action.toString()));
		}
		EntityManager pmgr = pmgrFactory.createEntityManager();
		pmgr.getTransaction().begin();
		try {
			org.apache.ode.runtime.exec.platform.task.TaskActionImpl t = new org.apache.ode.runtime.exec.platform.task.TaskActionImpl();
			t.setName(action);
			/*defer
			t.setInput(taskInput); 
			Set<TargetImpl> targetEntities = new HashSet<TargetImpl>();
			for (Target target : targets) {
				targetEntities.add((TargetImpl) target);
			}
			t.setTargets(targetEntities);
			//t.setComponent(ae.component);
			*/t.setNodeId(nodeId);
			t.setState(TaskAction.TaskActionState.SUBMIT);
			pmgr.persist(t);
			pmgr.getTransaction().commit();
			TaskActionInfo info = new TaskActionInfo();
			info.id = (TaskActionIdImpl) t.id();
			TaskActionCallable c = actionProvider.get();
			c.config(node, config, logLevel, correlationId, requestor, taskId, info.id, taskInput, executingActions);
			executingActions.put(correlationId, c);
			info.state = exec.submit(c);
			return info;
		} catch (PersistenceException pe) {
			throw new TaskException(pe);
		}

	}

	public Target[] ExchangeTypes(org.apache.ode.spi.exec.platform.xml.Task xmlTask) {
		List<Target> targets = new ArrayList<Target>();
		EntityManager pmgr = pmgrFactory.createEntityManager();
		try {
			for (JAXBElement<? extends org.apache.ode.spi.exec.platform.xml.Target> xmlTargetElement : xmlTask.getTargets().getTarget()) {
				org.apache.ode.spi.exec.platform.xml.Target xmlTarget = xmlTargetElement.getValue();
				if (xmlTarget instanceof org.apache.ode.spi.exec.platform.xml.TargetAll) {
					targets.add(pmgr.find(TargetAllImpl.class, new TargetPK(null, TargetAllImpl.TYPE)));
				} else if (xmlTarget instanceof org.apache.ode.spi.exec.platform.xml.TargetNode) {
					targets.add(pmgr.find(TargetNodeImpl.class, new TargetPK(((org.apache.ode.spi.exec.platform.xml.TargetNode) xmlTarget).getNodeId(),
							TargetNodeImpl.TYPE)));
				} else if (xmlTarget instanceof org.apache.ode.spi.exec.platform.xml.TargetCluster) {
					targets.add(pmgr.find(TargetClusterImpl.class, new TargetPK(
							((org.apache.ode.spi.exec.platform.xml.TargetCluster) xmlTarget).getClusterId(), TargetClusterImpl.TYPE)));
				} else {
					//throw new PlatformException(String.format("Invalid target type %s", xmlTarget.getClass().getName()));//TODO return error
				}
			}
		} finally {
			pmgr.close();//since the EntityManager is closed we don't need to detach the targets added above
		}
		return targets.toArray(new Target[targets.size()]);
	}

	public static Class<?> locateTaskTarget(Class<?> clazz, ExchangeType target) throws PlatformException {

		Class<?> targetClass = null;
		for (Type t : clazz.getGenericInterfaces()) {
			if (t instanceof ParameterizedType) {
				ParameterizedType pt = (ParameterizedType) t;
				if (TaskActionCoordinator.class.equals(pt.getRawType())) {
					if (target == ExchangeType.INPUT) {
						targetClass = (Class<?>) pt.getActualTypeArguments()[0];
						break;
					} else if (target == ExchangeType.OUTPUT) {
						targetClass = (Class<?>) pt.getActualTypeArguments()[3];
						break;
					}
				} else if (CoordinatedTaskActionCoordinator.class.equals(pt.getRawType())) {
					if (target == ExchangeType.INPUT) {
						targetClass = (Class<?>) pt.getActualTypeArguments()[0];
						break;
					} else if (target == ExchangeType.OUTPUT) {
						targetClass = (Class<?>) pt.getActualTypeArguments()[5];
						break;
					}
				}
			}
		}
		if (targetClass == null) {
			log.severe(String.format("Unable to convert target %s on task class %s", target.name(), clazz.getName()));
			throw new PlatformException(String.format("Unable to convert target %s on class %s", target.name(), clazz.getName()));
		}
		return targetClass;
	}

	public static Class<?> locateActionTarget(Class<?> clazz, ExchangeType target) throws PlatformException {

		Class<?> targetClass = null;
		for (Type t : clazz.getGenericInterfaces()) {
			if (t instanceof ParameterizedType) {
				ParameterizedType pt = (ParameterizedType) t;
				if (TaskActionCoordinator.class.equals(pt.getRawType())) {
					if (target == ExchangeType.INPUT) {
						targetClass = (Class<?>) pt.getActualTypeArguments()[1];
						break;
					} else if (target == ExchangeType.OUTPUT) {
						targetClass = (Class<?>) pt.getActualTypeArguments()[2];
						break;
					}
				} else if (CoordinatedTaskActionCoordinator.class.equals(pt.getRawType())) {
					if (target == ExchangeType.INPUT) {
						targetClass = (Class<?>) pt.getActualTypeArguments()[1];
						break;
					} else if (target == ExchangeType.OUTPUT) {
						targetClass = (Class<?>) pt.getActualTypeArguments()[4];
						break;
					} else if (target == ExchangeType.COORDINATE_INPUT) {
						targetClass = (Class<?>) pt.getActualTypeArguments()[2];
						break;
					} else if (target == ExchangeType.COORDINATE_OUTPUT) {
						targetClass = (Class<?>) pt.getActualTypeArguments()[3];
						break;
					}
				}
			}
		}
		if (targetClass == null) {
			log.severe(String.format("Unable to convert target %s on action class %s", target.name(), clazz.getName()));
			throw new PlatformException(String.format("Unable to convert target %s on class %s", target.name(), clazz.getName()));
		}
		return targetClass;
	}

	public static Object convertToObject(org.w3c.dom.Node node, Class<?> targetClazz, JAXBContext jaxbContext) throws PlatformException {
		if (node == null) {
			return null;
		}

		if (targetClazz.isAssignableFrom(Document.class)) {
			return node;
		}

		try {
			Unmarshaller u = jaxbContext.createUnmarshaller();
			if (targetClazz.isAssignableFrom(JAXBElement.class)) {
				return u.unmarshal(node);
			} else {
				JAXBElement e = u.unmarshal(node, targetClazz);
				return e.getValue();
			}

		} catch (Exception e) {
			log.log(Level.SEVERE, "", e);
			throw new PlatformException(e);
		}

	}

	public static Document convertToDocument(Element element) {
		if (element != null) {
			try {
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				dbf.setNamespaceAware(true);
				Document doc = dbf.newDocumentBuilder().newDocument();
				Element el = (Element) doc.adoptNode(element);
				doc.appendChild(el);
				return doc;
			} catch (Exception je) {
				log.log(Level.SEVERE, "", je);
			}
		}
		return null;

	}

	public static Element convertToElement(Object val, JAXBContext jaxbContext) throws PlatformException {
		Document doc = convertToDocument(val, jaxbContext);
		if (doc != null) {
			return doc.getDocumentElement();
		}
		return null;
	}

	public static Document convertToDocument(Object val, JAXBContext jaxbContext) throws PlatformException {
		if (val == null) {
			return null;
		}

		if (val instanceof Document) {
			return (Document) val;
		}
		try {
			Marshaller m = jaxbContext.createMarshaller();
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			Document doc = dbf.newDocumentBuilder().newDocument();
			if (val.getClass().getAnnotation(XmlRootElement.class) != null) {
				m.marshal(val, doc);
			} else {
				Class ofclass = val.getClass().getClassLoader().loadClass(String.format("%s.ObjectFactory", val.getClass().getPackage().getName()));
				Method meth = ofclass.getMethod(String.format("create%s", val.getClass().getSimpleName()), val.getClass());
				if (meth != null) {
					Object of = ofclass.newInstance();
					JAXBElement eval = (JAXBElement) meth.invoke(of, val);
					m.marshal(eval, doc);
				} else {
					throw new Exception(String.format("Can't find JAXB ObjectFactory method for %s", val.getClass().getName()));
				}
			}
			return doc;
		} catch (Exception je) {
			log.log(Level.SEVERE, "", je);
			throw new PlatformException(je);
		}
	}

	public void setupTasks(Collection<Component> components) throws PlatformException {
		/*// add built in actions
		ActionEntry installActionEntry = new ActionEntry(TaskType.MASTER, Platform.PLATFORM, installMasterActionProvider);
		installActionEntry.slave.add(new ActionEntry(TaskType.SLAVE, Platform.PLATFORM, installSlaveActionProvider));
		actions.put(PlatformAction.INSTALL_ACTION.qname(), installActionEntry);
		// add component actions
		if (components != null) {
			for (Component c : components) {
				if (c.actions() != null) {
					for (TaskActionImpl a : c.actions()) {
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
		}*/
	}

	/*
	ConcurrentHashMap<Long, ActionRunnable> getExecutingTasks() {
		return executingTasks;
	}

	public void run(org.apache.ode.runtime.exec.platform.task.TaskActionImpl action) throws PlatformException {
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
					for (TaskActionImpl a : c.actions()) {
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
			org.apache.ode.runtime.exec.platform.task.TaskActionImpl a = new org.apache.ode.runtime.exec.platform.task.TaskActionImpl();
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

	public ActionId executeMasterAction(QName action, Document actionInput, Set<String> targets) throws PlatformException {
		ActionEntry ae = actions.get(action);
		String masterTarget = null;
		Set<org.apache.ode.runtime.exec.platform.task.TaskActionImpl> localActions = new HashSet<org.apache.ode.runtime.exec.platform.task.TaskActionImpl>();
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
				for (org.apache.ode.runtime.exec.platform.task.TaskActionImpl la : localActions) {
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
			ActionStatus status = pmgr.find(org.apache.ode.runtime.exec.platform.task.TaskActionImpl.class, actionId.id);
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
					org.apache.ode.runtime.exec.platform.task.TaskActionImpl action = pmgr.find(org.apache.ode.runtime.exec.platform.task.TaskActionImpl.class, actionId.id);
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
		org.apache.ode.runtime.exec.platform.task.TaskActionImpl action;
		FutureTask<ActionRunnable> futureTask;
		CyclicBarrier refreshBarrier = new CyclicBarrier(2);
		Lock updateLock = new ReentrantLock();
		EntityManager pmgr = pmgrFactory.createEntityManager();
		volatile boolean canceled = false;

		public ActionRunnable(long actionId, ActionTask task) throws PlatformException {
			try {
				action = pmgr.find(org.apache.ode.runtime.exec.platform.task.TaskActionImpl.class, actionId);
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

		public org.apache.ode.runtime.exec.platform.task.TaskActionImpl getAction() {
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

		public boolean contextUpdate(org.apache.ode.runtime.exec.platform.task.TaskActionImpl action) throws PlatformException {
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
	*/
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
	/*
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
					context.log(new ActionMessage(new Date(), LogLevel.ERROR, pe.getMessage()));
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
					context.log(new ActionMessage(new Date(), LogLevel.ERROR, pe.getMessage()));
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
									context.log(new ActionMessage(new Date(), LogLevel.ERROR, pe.getMessage()));
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
					context.log(new ActionMessage(new Date(), LogLevel.ERROR, pe.getMessage()));
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
						context.log(new ActionMessage(new Date(), LogLevel.ERROR, pe.getMessage()));
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
		*/

}
