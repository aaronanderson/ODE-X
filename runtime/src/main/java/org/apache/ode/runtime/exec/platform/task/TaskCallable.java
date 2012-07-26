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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.PersistenceUnit;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.ode.runtime.exec.platform.MessageImpl;
import org.apache.ode.runtime.exec.platform.NodeImpl.NodeId;
import org.apache.ode.runtime.exec.platform.NodeImpl.TaskCheck;
import org.apache.ode.runtime.exec.platform.target.TargetImpl;
import org.apache.ode.runtime.exec.platform.task.TaskCallable.TaskResult;
import org.apache.ode.runtime.exec.platform.task.TaskImpl.TaskIdImpl;
import org.apache.ode.spi.exec.Message.LogLevel;
import org.apache.ode.spi.exec.Node;
import org.apache.ode.spi.exec.PlatformException;
import org.apache.ode.spi.exec.target.Target;
import org.apache.ode.spi.exec.task.CoordinatedTaskActionCoordinator;
import org.apache.ode.spi.exec.task.Task.TaskId;
import org.apache.ode.spi.exec.task.Task.TaskState;
import org.apache.ode.spi.exec.task.TaskActionCoordinator;
import org.apache.ode.spi.exec.task.TaskActionCoordinator.TaskActionRequest;
import org.apache.ode.spi.exec.task.TaskActionCoordinator.TaskActionResponse;
import org.apache.ode.spi.exec.task.TaskActionDefinition;
import org.apache.ode.spi.exec.task.TaskCallback;
import org.apache.ode.spi.exec.task.TaskContext;
import org.apache.ode.spi.exec.task.TaskDefinition;
import org.w3c.dom.Document;

public class TaskCallable implements Callable<TaskResult> {
	/**
	 * 
	 */
	@NodeId
	String nodeId;

	@PersistenceUnit(unitName = "platform")
	EntityManagerFactory pmgrFactory;

	@TaskCheck
	QueueConnectionFactory queueConFactory;

	@TaskCheck
	private Queue taskQueue;

	private QueueSender taskActionSender;
	private QueueConnection taskUpdateQueueConnection;
	private QueueSession taskUpdateSession;

	private Queue taskRequestor;
	private QueueSender taskRequestorSender;
	private String taskCorrelationId;

	Map<QName, TaskDefinition<?, ?, ?, ?>> tasks;
	Map<QName, TaskActionDefinition<?, ?>> actions;

	private org.apache.ode.runtime.exec.cluster.xml.TaskCheck config;
	private TaskIdImpl id;
	private TaskContextImpl ctx;
	private TaskCallback<?, ?> callback;
	private Document taskInput;
	private Target[] targets;
	private EntityManager pmgr;
	private TaskImpl task;
	private LogLevel logLevel;

	private static final Logger log = Logger.getLogger(TaskCallable.class.getName());

	public void config(Node node, org.apache.ode.runtime.exec.cluster.xml.TaskCheck config, LogLevel logLevel, Queue taskRequestor, String taskCorrelationId,
			TaskIdImpl id, Document taskInput, TaskCallback<?, ?> callback, Target... targets) {
		this.config = config;
		this.taskRequestor = taskRequestor;
		this.taskCorrelationId = taskCorrelationId;
		this.id = id;
		this.ctx = new TaskContextImpl();
		this.taskInput = taskInput;
		this.callback = callback;
		this.targets = targets;
		this.tasks = node.getTaskDefinitions();
		this.actions = node.getTaskActionDefinitions();
		this.logLevel = logLevel;
	}

	public class TaskActionExecution {
		TaskActionCoordinator<?, ?, ?, ?> owner;
		TaskActionImpl action;
		TaskActionResponse<?> response;
		TaskActionRequest<?> request;
		Set<TaskActionExecution> prevDependencies = new HashSet<TaskActionExecution>();
		Set<TaskActionExecution> nextDependencies = new HashSet<TaskActionExecution>();
		private Queue actionRequestor;
		private QueueReceiver actionRequestorReceiver;
		private String actionCorrelationId;

	}

	public class TaskCoordinatorState {
		TaskActionCoordinator<?, ?, ?, ?> coordinator;
		Set<TaskActionExecution> requests = new HashSet<TaskActionExecution>();
	}

	public static class TaskResult {
		public Document output;
		public TaskState state;
	}

	@Override
	public TaskResult call() throws PlatformException {
		pmgr = pmgrFactory.createEntityManager();
		try {
			task = pmgr.find(TaskImpl.class, id.id());
			try {
				taskUpdateQueueConnection = queueConFactory.createQueueConnection();
				taskUpdateSession = taskUpdateQueueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
				taskActionSender = taskUpdateSession.createSender(taskQueue);
				if (taskRequestor != null) {
					taskRequestorSender = taskUpdateSession.createSender(taskRequestor);
				}
			} catch (JMSException je) {
				log.log(Level.SEVERE, "", je);
				taskLogIt(LogLevel.ERROR, 0, je.getMessage(), true);

			}

			Set<TaskCoordinatorState> coordinators = new HashSet<TaskCoordinatorState>();
			Map<QName, TaskActionExecution> actionExecutions = new HashMap<QName, TaskActionExecution>();
			init(coordinators, actionExecutions);

			while (true) {
				//listen for action updates (filter on task ID), storing them in DB
				//listen for message updates (filter on task ID), logging them in DB
				//(optional) send external Task updates to queue 
				//iterate through actions, submitting actions to queue
				break;

			}
			taskUpdateQueueConnection.close();

		} catch (JMSException je) {
			log.log(Level.SEVERE, "", je);
			taskLogIt(LogLevel.ERROR, 0, je.getMessage(), true);

		} finally {
			pmgr.close();

		}
		TaskResult result = new TaskResult();
		result.output = null;
		result.state = task.state();
		return result;
	}

	public class TaskContextImpl implements TaskContext {
		boolean failed = false;

		@Override
		public TaskId id() {
			return task.id();
		}

		@Override
		public QName name() {
			return task.name();
		}

		@Override
		public void log(LogLevel level, int code, String message) {
			log(level, code, message);

		}

		@Override
		public TaskState getState() {
			return task.state();
		}

		@Override
		public void setFailed() {
			failed = true;

		}

	}

	public void init(Set<TaskCoordinatorState> coordinators, Map<QName, TaskActionExecution> actionExecutions) throws PlatformException {

		TaskDefinition<?, ?, ?, ?> def = tasks.get(task.name());

		for (TaskActionCoordinator coordinator : def.coordinators()) {
			TaskCoordinatorState taskCordState = new TaskCoordinatorState();
			taskCordState.coordinator = coordinator;
			Set<TaskActionRequest<?>> requests = coordinator.init(ctx,
					convertToObject(taskInput, locateTarget(coordinator.getClass(), ConvertTarget.INPUT), def.jaxbContext()), nodeId, callback, targets);
			for (TaskActionRequest<?> canidate : requests) {
				if (!actions.containsKey(canidate.action)) {
					taskLogIt(LogLevel.ERROR, 0, String.format("Unsupported TaskAction %s", canidate.action.toString()), true);
				}
				if (actionExecutions.containsKey(canidate.action)) {
					taskLogIt(LogLevel.ERROR, 0, String.format("Duplicate TaskAction %s", canidate.action.toString()), true);
				}
				TaskActionExecution tae = new TaskActionExecution();
				tae.owner = coordinator;
				tae.request = canidate;
				try {
					tae.actionRequestor = taskUpdateSession.createTemporaryQueue();
					tae.actionRequestorReceiver = taskUpdateSession.createReceiver(tae.actionRequestor);
				} catch (JMSException e) {
					taskLogIt(LogLevel.ERROR, 0, e.getMessage(), true);
				}
				taskCordState.requests.add(tae);
				actionExecutions.put(canidate.action, tae);

			}
			coordinators.add(taskCordState);
		}

		//calculate run order

		for (TaskCoordinatorState coordState : coordinators) {
			for (TaskActionExecution actionExec : coordState.requests) {
				for (QName dep : actions.get(actionExec.request.action).dependencies()) {
					TaskActionExecution depActionExec = actionExecutions.get(dep);
					if (depActionExec == null) {
						taskLogIt(LogLevel.ERROR, 0,
								String.format("TaskAction %s dependency %s not submitted for execution by a task coordinator", actionExec.request.action, dep),
								true);
					}
					//TODO detect loops
					actionExec.prevDependencies.add(depActionExec);
					depActionExec.nextDependencies.add(actionExec);
				}

			}
		}

		//now Task has been vetted, perform IO intensive operations
		pmgr.getTransaction().begin();
		try {
			for (TaskActionExecution actionExec : actionExecutions.values()) {
				//actionExec.receiver = taskUpdateSession.createReceiver(taskQueue, String.format(Node.NODE_MQ_FILTER_TASK, nodeId, id.id()));
				//actionExec.sender = taskUpdateSession.createSender(taskQueue);

			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "", e);
			taskLogIt(LogLevel.ERROR, 0, e.getMessage(), true);

		}

		pmgr.getTransaction().begin();
		try {
			task.setInput(taskInput);
			Set<TargetImpl> targetEntities = new HashSet<TargetImpl>();
			for (Target target : targets) {
				targetEntities.add((TargetImpl) target);
			}
			task.setTargets(targetEntities);
			//t.setComponent(ae.component);
			pmgr.merge(task);
			pmgr.getTransaction().commit();
		} catch (PersistenceException pe) {
			pmgr.getTransaction().rollback();
			throw new PlatformException(pe);
		}
	}

	public void taskLogIt(LogLevel level, int code, String msg, boolean error) throws PlatformException {
		pmgr.getTransaction().begin();
		try {
			if (error) {
				task.setState(TaskState.FAIL);
			}
			MessageImpl m = new MessageImpl();
			m.setLevel(level.toString());
			m.setCode(code);
			m.setMessage(msg);
			m.setTimestamp(Calendar.getInstance().getTime());
			task.messages().add(m);
			pmgr.merge(task);
			pmgr.getTransaction().commit();
		} catch (PersistenceException pe) {
			throw new PlatformException(pe);
		} finally {
			pmgr.close();
		}
		if (error) {
			throw new PlatformException(msg);
		}
	}

	public static enum ConvertTarget {
		INPUT, OUTPUT, COORDINATE_INPUT, COORDINATE_OUTPUT;
	}

	public static Class<?> locateTarget(Class<?> clazz, ConvertTarget target) throws PlatformException {

		Class<?> targetClass = null;
		for (Type t : clazz.getGenericInterfaces()) {
			if (t instanceof ParameterizedType) {
				ParameterizedType pt = (ParameterizedType) t;
				if (TaskActionCoordinator.class.equals(pt.getRawType())) {
					if (target == ConvertTarget.INPUT) {
						targetClass = (Class<?>) pt.getActualTypeArguments()[0];
						break;
					} else if (target == ConvertTarget.OUTPUT) {
						targetClass = (Class<?>) pt.getActualTypeArguments()[1];
						break;
					}
				} else if (CoordinatedTaskActionCoordinator.class.equals(pt.getRawType())) {
					if (target == ConvertTarget.COORDINATE_INPUT) {
						targetClass = (Class<?>) pt.getActualTypeArguments()[0];
						break;
					} else if (target == ConvertTarget.COORDINATE_OUTPUT) {
						targetClass = (Class<?>) pt.getActualTypeArguments()[1];
						break;
					}
				}
			}
		}
		if (targetClass == null) {
			throw new PlatformException(String.format("Unable to convert target %s on class %s", target.name(), clazz.getName()));
		}
		return targetClass;
	}

	public static Object convertToObject(Document doc, Class<?> targetClazz, JAXBContext jaxbContext) throws PlatformException {

		if (targetClazz.isAssignableFrom(Document.class)) {
			return doc;
		}
		try {
			Unmarshaller u = jaxbContext.createUnmarshaller();
			JAXBElement e = u.unmarshal(doc, targetClazz);
			return e.getValue();
		} catch (JAXBException je) {
			throw new PlatformException(je);
		}

	}

	public static Document convertToDocument(Object val, Class<?> targetClazz, JAXBContext jaxbContext) throws PlatformException {
		if (val instanceof Document) {
			return (Document) val;
		}
		try {
			Marshaller m = jaxbContext.createMarshaller();
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			Document doc = dbf.newDocumentBuilder().newDocument();
			m.marshal(val, doc);
			return doc;
		} catch (Exception je) {
			throw new PlatformException(je);
		}
	}

}