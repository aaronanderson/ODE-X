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

import static org.apache.ode.runtime.exec.platform.MessageHandler.log;
import static org.apache.ode.runtime.exec.platform.NodeImpl.CLUSTER_JAXB_CTX;
import static org.apache.ode.runtime.exec.platform.task.TaskExecutor.convertToDocument;
import static org.apache.ode.runtime.exec.platform.task.TaskExecutor.convertToElement;
import static org.apache.ode.runtime.exec.platform.task.TaskExecutor.convertToObject;
import static org.apache.ode.runtime.exec.platform.task.TaskExecutor.locateActionTarget;
import static org.apache.ode.runtime.exec.platform.task.TaskExecutor.locateTaskTarget;
import static org.apache.ode.spi.exec.Node.CLUSTER_NAMESPACE;
import static org.apache.ode.spi.exec.Node.NODE_MQ_CORRELATIONID_ACTION;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.PersistenceUnit;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessOrder;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import org.apache.ode.runtime.exec.cluster.xml.ExchangeType;
import org.apache.ode.runtime.exec.cluster.xml.TaskAction.Exchange;
import org.apache.ode.runtime.exec.platform.MessageImpl;
import org.apache.ode.runtime.exec.platform.NodeImpl.MessageCheck;
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
import org.apache.ode.spi.exec.task.CoordinatedTaskActionCoordinator.TaskActionCoordinationRequest;
import org.apache.ode.spi.exec.task.CoordinatedTaskActionCoordinator.TaskActionCoordinationResponse;
import org.apache.ode.spi.exec.task.Task.TaskId;
import org.apache.ode.spi.exec.task.Task.TaskState;
import org.apache.ode.spi.exec.task.TaskAction.TaskActionState;
import org.apache.ode.spi.exec.task.TaskActionCoordinator;
import org.apache.ode.spi.exec.task.TaskActionCoordinator.TaskActionRequest;
import org.apache.ode.spi.exec.task.TaskActionCoordinator.TaskActionResponse;
import org.apache.ode.spi.exec.task.TaskActionDefinition;
import org.apache.ode.spi.exec.task.TaskCallback;
import org.apache.ode.spi.exec.task.TaskContext;
import org.apache.ode.spi.exec.task.TaskDefinition;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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

	@MessageCheck
	TopicConnectionFactory msgTopicConFactory;

	@MessageCheck
	Topic msgUpdateTopic;

	private QueueSender taskActionSender;
	private QueueConnection taskUpdateQueueConnection;
	private QueueSession taskUpdateSession;

	private TopicConnection msgUpdateTopicConnection;
	private TopicSession msgUpdateSession;
	private TopicPublisher msgUpdatePublisher;

	private Queue taskRequestor;
	private QueueSender taskRequestorSender;
	private String taskCorrelationId;

	Map<QName, TaskDefinition<?, ?>> tasks;
	Map<QName, TaskActionDefinition<?, ?>> actions;

	private org.apache.ode.runtime.exec.cluster.xml.TaskCheck config;
	private TaskIdImpl taskId;
	private TaskContextImpl taskContext;
	private TaskCallback<?, ?> callback;
	private Element taskInput;
	private Target[] targets;
	private EntityManager pmgr;
	private TaskImpl task;
	private LogLevel logLevel;

	private LinkedList<org.apache.ode.runtime.exec.cluster.xml.Message> msgQueue = new LinkedList<org.apache.ode.runtime.exec.cluster.xml.Message>();

	private static final Logger log = Logger.getLogger(TaskCallable.class.getName());

	public void config(Node node, org.apache.ode.runtime.exec.cluster.xml.TaskCheck config, LogLevel logLevel, String taskCorrelationId, Queue taskRequestor,
			TaskIdImpl taskId, Element taskInput, TaskCallback<?, ?> callback, Target... targets) {
		this.config = config;
		this.taskRequestor = taskRequestor;
		this.taskCorrelationId = taskCorrelationId;
		this.taskId = taskId;
		this.taskContext = new TaskContextImpl();
		this.taskInput = taskInput;
		this.callback = callback;
		this.targets = targets;
		this.tasks = node.getTaskDefinitions();
		this.actions = node.getTaskActionDefinitions();
		this.logLevel = logLevel;
	}

	public class TaskActionExecution {
		TaskCoordinatorExecution owner;
		//TaskActionImpl action;
		TaskActionState state = TaskActionState.SUBMIT;
		TaskActionState previousState;
		long lastUpdate = 0l;
		String taskActionId = Node.NODE_MQ_PROP_VALUE_NEW;
		Element actionCoordinationOutput;
		Element actionOutput;
		JAXBContext actionJaxbContext;
		boolean addedTaskAction = false;
		TaskActionResponse response;
		TaskActionRequest request;
		Set<TaskActionExecution> prevDependencies = new HashSet<TaskActionExecution>();
		//Set<TaskActionExecution> nextDependencies = new HashSet<TaskActionExecution>();
		private Queue actionRequestor;
		private String actionCorrelationId;
		private QueueReceiver actionRequestorReceiver;

	}

	public class TaskCoordinatorExecution {
		TaskActionCoordinator coordinator;
		Set<TaskActionExecution> requests = new HashSet<TaskActionExecution>();
		JAXBContext taskJaxbContext;
		boolean initialized = false;
		boolean finialized = false;
	}

	public static class TaskResult {
		public Document output;
		public TaskState state;
	}

	@Override
	public TaskResult call() throws PlatformException {
		Document output = null;
		try {
			pmgr = pmgrFactory.createEntityManager();
			task = pmgr.find(TaskImpl.class, taskId.id());

			try {
				taskUpdateQueueConnection = queueConFactory.createQueueConnection();
				msgUpdateTopicConnection = msgTopicConFactory.createTopicConnection();

				//try {
				taskUpdateSession = taskUpdateQueueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
				taskActionSender = taskUpdateSession.createSender(taskQueue);
				if (taskRequestor != null) {
					taskRequestorSender = taskUpdateSession.createSender(taskRequestor);
				}

				msgUpdateSession = msgUpdateTopicConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
				msgUpdatePublisher = msgUpdateSession.createPublisher(msgUpdateTopic);

				taskUpdateQueueConnection.start();
				msgUpdateTopicConnection.start();

				Map<QName, TaskCoordinatorExecution> coordinators = new HashMap<QName, TaskCoordinatorExecution>();
				Map<QName, TaskActionExecution> actionExecutions = new HashMap<QName, TaskActionExecution>();
				try {
					init(coordinators, actionExecutions);
					do {
						for (TaskActionExecution tae : actionExecutions.values()) {
							refreshAction(tae);
							executeAction(tae);
						}
					} while (!complete(actionExecutions.values()));
				} finally {
					output = destroy(coordinators, actionExecutions);
				}

			} catch (JMSException je) {
				log.log(Level.SEVERE, "", je);
				taskLogIt(LogLevel.ERROR, 0, je.getMessage(), true);

			} finally {
				try {
					taskUpdateQueueConnection.close();
				} catch (JMSException e) { //don't care about JMS errors on closure
				}
				try {
					msgUpdateTopicConnection.close();
				} catch (JMSException e) { //don't care about JMS errors on closure
				}
			}
		} finally {
			pmgr.close();

		}
		TaskResult result = new TaskResult();
		result.output = output;
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

	public void init(Map<QName, TaskCoordinatorExecution> coordinators, Map<QName, TaskActionExecution> actionExecutions) throws PlatformException {

		TaskDefinition<?, ?> def = tasks.get(task.name());
		if (def == null) {
			taskLogIt(LogLevel.ERROR, 0, String.format("No TaskDefinition defined for task %s", task.name()), true);
		}

		task.setState(TaskState.START);
		task.setStart(new Date());
		updateTask();

		for (TaskActionCoordinator coordinator : def.coordinators()) {
			TaskCoordinatorExecution taskCordExec = new TaskCoordinatorExecution();
			taskCordExec.taskJaxbContext = def.jaxbContext();
			taskCordExec.coordinator = coordinator;
			taskCordExec.initialized = false;
			taskCordExec.finialized = false;
			coordinators.put(coordinator.name(), taskCordExec);
		}
		/*if (coordinators.size() == 0) {
			taskLogIt(LogLevel.ERROR, 0, String.format("No TaskCoordinators registered for task %s", task.name()), true);
		} can't happen*/
		for (TaskCoordinatorExecution coordExec : (coordinators.values())) {
			for (QName dep : (Set<QName>) coordExec.coordinator.dependencies()) {
				if (!coordinators.containsKey(dep)) {
					taskLogIt(LogLevel.ERROR, 0, String.format("TaskCoordinator %s dependency %s not registered", coordExec.coordinator.name(), dep), true);
				}
			}
		}
		boolean finished = false;
		while (!finished) {
			finished = true;
			boolean modified = false;
			Set<QName> remaining = new HashSet<QName>();
			for (TaskCoordinatorExecution coordExec : (coordinators.values())) {
				boolean dependenciesCompleted = true;
				for (QName depExecName : (Set<QName>) coordExec.coordinator.dependencies()) {
					if (!coordinators.get(depExecName).initialized) {
						dependenciesCompleted = false;
						remaining.add(depExecName);
						break;
					}
				}
				if (dependenciesCompleted) {
					Set<TaskActionRequest<?>> requests = coordExec.coordinator.init(taskContext,
							convertToObject(taskInput, locateTaskTarget(coordExec.coordinator.getClass(), ExchangeType.INPUT), coordExec.taskJaxbContext),
							nodeId, callback, targets);

					for (TaskActionRequest<?> canidate : requests) {
						if (!actions.containsKey(canidate.action)) {
							taskLogIt(LogLevel.ERROR, 0, String.format("Unsupported TaskAction %s", canidate.action.toString()), true);
						}
						if (actionExecutions.containsKey(canidate.action)) {
							taskLogIt(LogLevel.ERROR, 0, String.format("Duplicate TaskAction %s", canidate.action.toString()), true);
						}
						TaskActionExecution tae = new TaskActionExecution();
						tae.owner = coordExec;
						tae.request = canidate;
						tae.actionJaxbContext = actions.get(canidate.action).jaxbContext();
						try {
							tae.actionRequestor = taskUpdateSession.createTemporaryQueue();
							tae.actionRequestorReceiver = taskUpdateSession.createReceiver(tae.actionRequestor);
							tae.actionCorrelationId = String.format(NODE_MQ_CORRELATIONID_ACTION, System.currentTimeMillis());
						} catch (JMSException e) {
							taskLogIt(LogLevel.ERROR, 0, e.getMessage(), true);
						}
						coordExec.requests.add(tae);
						actionExecutions.put(canidate.action, tae);
					}
					modified = true;
				} else {
					finished = false;
				}
			}
			if (!modified) {
				taskLogIt(LogLevel.ERROR, 0, String.format("TaskCoordinator circular dependency detected: %s", remaining), true);
			}

		}

		Map<QName, Boolean> resolved = new HashMap<QName, Boolean>();
		finished = false;
		while (!finished) {
			finished = true;
			boolean modified = false;
			Set<QName> remaining = new HashSet<QName>();
			for (TaskActionExecution actionExec : actionExecutions.values()) {
				if (resolved.get(actionExec.request.action) != null) {
					if (resolved.get(actionExec.request.action)) {
						continue;
					}
					boolean dependenciesResolved = true;
					for (QName dep : actions.get(actionExec.request.action).dependencies()) {
						if (resolved.get(dep)) {
							continue;
						} else {
							remaining.add(dep);
							dependenciesResolved = false;
							finished = false;
							break;
						}
					}
					if (dependenciesResolved) {
						resolved.put(actionExec.request.action, true);
						modified = true;
					}
				} else {
					resolved.put(actionExec.request.action, false);
					finished = false;
					modified = true;
					for (QName dep : actions.get(actionExec.request.action).dependencies()) {
						TaskActionExecution depActionExec = actionExecutions.get(dep);
						if (depActionExec == null) {
							taskLogIt(LogLevel.ERROR, 0, String.format("TaskAction %s dependency %s not submitted for execution by a task coordinator",
									actionExec.request.action, dep), true);
						}
						actionExec.prevDependencies.add(depActionExec);
						//depActionExec.nextDependencies.add(actionExec);
					}
				}

			}
			if (!modified) {
				taskLogIt(LogLevel.ERROR, 0, String.format("TaskAction circular dependency detected: %s", remaining), true);

			}
		}
		//now Task has been vetted, perform IO intensive operations

		pmgr.getTransaction().begin();
		try {
			task.setState(TaskState.EXECUTE);
			task.setDOMInput(convertToDocument(taskInput));
			Set<TargetImpl> targetEntities = new HashSet<TargetImpl>();
			for (Target target : targets) {
				TargetImpl targetImpl = (TargetImpl) target;
				pmgr.merge(targetImpl);
				targetEntities.add(targetImpl);
			}
			task.setTargets(targetEntities);
			//t.setComponent(ae.component);
			pmgr.merge(task);
			pmgr.getTransaction().commit();
		} catch (PersistenceException pe) {
			//pmgr.getTransaction().rollback();
			throw new PlatformException(pe);
		}
	}

	public void executeAction(TaskActionExecution tae) throws PlatformException {
		if (System.currentTimeMillis() - tae.lastUpdate > config.getActionTimeout()) {
			taskLogIt(LogLevel.ERROR, 0, String.format("TaskAction timed out: %s %s", tae.request.action, tae.request.nodeId), false);
			tae.response = new TaskActionResponse(tae.request.action, tae.request.nodeId, null, false);
		}
		org.apache.ode.runtime.exec.cluster.xml.TaskAction xmlTaskAction = convert(tae);

		try {
			switch (tae.state) {
			case SUBMIT:
				boolean ready = true;
				for (TaskActionExecution dep : tae.prevDependencies) {
					if (isExecuting(dep)) {
						ready = false;
						break;
					}
				}
				if (ready && tae.previousState != TaskActionState.SUBMIT) {
					Set<TaskActionResponse> dependencies = new HashSet<TaskActionResponse>();
					for (TaskActionExecution dep : tae.prevDependencies) {
						dependencies.add(dep.response);
					}
					if (dependencies.size() > 0) {
						tae.owner.coordinator.update(tae.request, dependencies);
					}
					xmlTaskAction.setState(org.apache.ode.runtime.exec.cluster.xml.TaskActionState.SUBMIT);
					if (tae.request.input != null) {
						xmlTaskAction.setExchange(new Exchange());
						xmlTaskAction.getExchange().setType(ExchangeType.INPUT);
						xmlTaskAction.getExchange().setPayload(convertToElement(tae.request.input, tae.actionJaxbContext));
					}
					updateTaskAction(xmlTaskAction, tae.actionCorrelationId, tae.actionRequestor);
					tae.previousState = TaskActionState.SUBMIT;//so we don't resend the same request before an update from the action executor
				}
				break;
			case START:
				if (!tae.addedTaskAction) {
					pmgr.getTransaction().begin();
					try {
						TaskActionImpl taskAction = pmgr.find(TaskActionImpl.class, Long.valueOf(xmlTaskAction.getActionId()));
						if (taskAction != null) {
							task.getActions().add(taskAction);
							pmgr.getTransaction().commit();
						} else {
							log.warning(String.format("Unable to find TaskAction %s for Task %s", xmlTaskAction.getActionId(), taskId));
							pmgr.getTransaction().rollback();
						}
						tae.addedTaskAction = true;
					} catch (PersistenceException pe) {
						log.log(Level.SEVERE, "", pe);
					}
				}
				break;
			case EXECUTE:
				if (tae.owner instanceof CoordinatedTaskActionCoordinator
						&& (tae.actionCoordinationOutput != null || tae.previousState == TaskActionState.START)) {
					Object input = convertToObject(tae.actionCoordinationOutput,
							locateActionTarget(tae.owner.coordinator.getClass(), ExchangeType.COORDINATE_INPUT), tae.owner.taskJaxbContext);

					TaskActionCoordinationResponse cres = new TaskActionCoordinationResponse(tae.request.action, tae.request.nodeId, input, false);
					TaskActionCoordinationRequest creq = ((CoordinatedTaskActionCoordinator) tae.owner).coordinate(cres);
					tae.actionCoordinationOutput = null;
					xmlTaskAction.setState(org.apache.ode.runtime.exec.cluster.xml.TaskActionState.EXECUTE);
					xmlTaskAction.setExchange(new Exchange());
					xmlTaskAction.getExchange().setType(ExchangeType.COORDINATE_INPUT);
					xmlTaskAction.getExchange().setPayload(convertToElement(creq.input, tae.actionJaxbContext));
					updateTaskAction(xmlTaskAction, tae.actionCorrelationId, tae.actionRequestor);
				}
				break;
			case PENDING:
				Object output = tae.actionOutput != null ? convertToObject(tae.actionOutput,
						locateActionTarget(tae.owner.coordinator.getClass(), ExchangeType.OUTPUT), tae.actionJaxbContext) : null;
				tae.response = new TaskActionResponse(tae.request.action, tae.request.nodeId, output, true);
				break;
			/*case FINISH:
			break;
			
			case COMMIT:
			break;
			case ROLLBACK:
			break;*/
			case COMPLETE:
				if (tae.previousState != TaskActionState.PENDING) {
					output = tae.actionOutput != null ? convertToObject(tae.actionOutput,
							locateActionTarget(tae.owner.coordinator.getClass(), ExchangeType.OUTPUT), tae.actionJaxbContext) : null;
					tae.response = new TaskActionResponse(tae.request.action, tae.request.nodeId, output, true);
				}
				break;

			case FAILED:
				output = tae.actionOutput != null ? convertToObject(tae.actionOutput,
						locateActionTarget(tae.owner.coordinator.getClass(), ExchangeType.OUTPUT), tae.actionJaxbContext) : null;
				tae.response = new TaskActionResponse(tae.request.action, tae.request.nodeId, output, false);
				break;

			}
		} catch (PlatformException pe) {
			tae.previousState = tae.state;
			tae.state = TaskActionState.FAILED;
			throw pe;
		}

	}

	public boolean complete(Collection<TaskActionExecution> taes) throws PlatformException {

		boolean finished = true;
		for (TaskActionExecution tae : taes) {
			if (tae.state != TaskActionState.PENDING) {
				finished &= !isExecuting(tae);
			}
		}
		return finished;
	}

	public Document destroy(Map<QName, TaskCoordinatorExecution> coordinators, Map<QName, TaskActionExecution> actionExecutions) throws PlatformException {

		Document doc = null;

		task.setState(TaskState.FINISH);
		updateTask();

		boolean finished = false;
		while (!finished) {//dependency loops should have already been resolved
			finished = true;
			Set<QName> remaining = new HashSet<QName>();
			for (TaskCoordinatorExecution coordExec : (coordinators.values())) {
				boolean dependenciesCompleted = true;
				for (QName depExecName : (Set<QName>) coordExec.coordinator.dependencies()) {
					if (coordinators.get(depExecName).initialized && !coordinators.get(depExecName).finialized) {
						dependenciesCompleted = false;
						remaining.add(depExecName);
						break;
					}
				}
				if (dependenciesCompleted) {
					Object output = convertToObject(doc, locateTaskTarget(coordExec.coordinator.getClass(), ExchangeType.OUTPUT), coordExec.taskJaxbContext);
					Set<TaskActionResponse> responses = new HashSet<TaskActionResponse>();
					for (TaskActionExecution tae : coordExec.requests) {
						if (tae.response != null) {
							responses.add(tae.response);
						} else {
							responses.add(new TaskActionResponse(tae.request.action, tae.request.nodeId, null, false));
						}
					}
					output = coordExec.coordinator.finish(responses, output);
					doc = convertToDocument(output, coordExec.taskJaxbContext);
				} else {
					finished = false;
				}
			}
		}

		Set<TaskActionExecution> pending = new HashSet<TaskActionExecution>();
		for (TaskActionExecution tae : actionExecutions.values()) {
			if (tae.state == TaskActionState.PENDING) {
				pending.add(tae);
				org.apache.ode.runtime.exec.cluster.xml.TaskAction xmlTaskAction = convert(tae);
				xmlTaskAction.setState(taskContext.failed ? org.apache.ode.runtime.exec.cluster.xml.TaskActionState.ROLLBACK
						: org.apache.ode.runtime.exec.cluster.xml.TaskActionState.COMMIT);
				updateTaskAction(xmlTaskAction, tae.actionCorrelationId, tae.actionRequestor);
			}
		}

		for (TaskActionExecution tae : pending) {
			refreshAction(tae, true);
			if (tae.state == TaskActionState.PENDING) {
				//this should not happen
			}
		}

		for (TaskActionExecution tae : actionExecutions.values()) {
			try {
				if (tae.actionRequestorReceiver != null) {
					tae.actionRequestorReceiver.close();
				}
			} catch (JMSException e) {//don't care on close
			}
		}

		task.setState(TaskState.COMPLETE);
		task.setFinish(new Date());
		task.setDOMOutput(doc);
		updateTask();

		return doc;
	}

	public void refreshAction(TaskActionExecution tae) throws PlatformException {
		refreshAction(tae, false);
	}

	public void refreshAction(TaskActionExecution tae, boolean block) throws PlatformException {
		if (isExecuting(tae)) {
			try {
				BytesMessage message;
				if (block) {
					message = (BytesMessage) tae.actionRequestorReceiver.receive(config.getActionTimeout());
				} else {
					message = (BytesMessage) tae.actionRequestorReceiver.receiveNoWait();
				}
				if/*while*/(message != null) {
					if (tae.actionCorrelationId.equals(message.getJMSCorrelationID())) {
						byte[] payload = new byte[(int) message.getBodyLength()];
						message.readBytes(payload);
						Unmarshaller umarshaller = CLUSTER_JAXB_CTX.createUnmarshaller();
						JAXBElement<org.apache.ode.runtime.exec.cluster.xml.TaskAction> element = umarshaller.unmarshal(new StreamSource(
								new ByteArrayInputStream(payload)), org.apache.ode.runtime.exec.cluster.xml.TaskAction.class);
						org.apache.ode.runtime.exec.cluster.xml.TaskAction xmlTaskAction = element.getValue();
						tae.taskActionId = xmlTaskAction.getActionId();
						tae.previousState = tae.state;
						tae.state = TaskActionState.valueOf(xmlTaskAction.getState().value());
						tae.lastUpdate = xmlTaskAction.getModified().getTimeInMillis();
						for (org.apache.ode.runtime.exec.cluster.xml.Message msg : xmlTaskAction.getTaskActionMessages().getTaskActionMessage()) {
							//TODO update task listener
						}
						if (xmlTaskAction.getExchange() != null && xmlTaskAction.getExchange().getType() == ExchangeType.OUTPUT) {
							tae.actionOutput = xmlTaskAction.getExchange().getPayload();
						} else if (xmlTaskAction.getExchange() != null && xmlTaskAction.getExchange().getType() == ExchangeType.COORDINATE_OUTPUT) {
							tae.actionCoordinationOutput = xmlTaskAction.getExchange().getPayload();
						}
						//message = (BytesMessage) tae.actionRequestorReceiver.receiveNoWait();
					} else {
						log.log(Level.WARNING, String.format("Received message without expected correlationId %s :%s", tae.actionCorrelationId, message));
					}
				}
			} catch (Exception e) {
				tae.state = TaskActionState.FAILED;
				taskLogIt(LogLevel.ERROR, 0, e.getMessage(), false);
			}
		}
	}

	public boolean isExecuting(TaskActionExecution tae) {
		return tae.state == TaskActionState.COMPLETE || tae.state == TaskActionState.FAILED ? false : true;
	}

	public void taskLogIt(LogLevel level, int code, String msg, boolean error) throws PlatformException {
		MessageImpl m = new MessageImpl();
		pmgr.getTransaction().begin();
		try {
			m.setLevel(level.toString());
			m.setCode(code);
			m.setMessage(msg);
			m.setTimestamp(Calendar.getInstance().getTime());
			task.messages().add(m);
			pmgr.merge(task);
			pmgr.getTransaction().commit();
		} catch (PersistenceException pe) {
			//pmgr.getTransaction().rollback();
			throw new PlatformException(pe);
		}
		//TODO Upate task listeners
		log(m, logLevel, msgQueue, msgUpdateSession, taskCorrelationId, msgUpdatePublisher);
		if (error) {
			task.setState(TaskState.FAIL);
			updateTask();
			throw new PlatformException(msg);
		}
	}

	public void updateTask() throws PlatformException {
		pmgr.getTransaction().begin();
		try {
			pmgr.merge(task);
			pmgr.getTransaction().commit();
		} catch (PersistenceException pe) {
			log.log(Level.SEVERE, "", pe);
		}
		if (taskRequestorSender != null) {
			try {
				org.apache.ode.runtime.exec.cluster.xml.Task xmlTask = convert(task);
				BytesMessage jmsMessage = taskUpdateSession.createBytesMessage();
				Marshaller marshaller = CLUSTER_JAXB_CTX.createMarshaller();
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				marshaller.marshal(new JAXBElement(new QName(CLUSTER_NAMESPACE, "Task"), org.apache.ode.runtime.exec.cluster.xml.Task.class, xmlTask), bos);
				jmsMessage.writeBytes(bos.toByteArray());
				taskRequestorSender.send(jmsMessage);
			} catch (Exception je) {
				log.log(Level.SEVERE, "", je);
				throw new PlatformException(je);
			}
		}
	}

	public void updateTaskAction(org.apache.ode.runtime.exec.cluster.xml.TaskAction xmlTaskAction, String actionCorrelationId, Queue actionRequestor)
			throws PlatformException {
		try {
			BytesMessage jmsMessage = taskUpdateSession.createBytesMessage();
			jmsMessage.setJMSCorrelationID(actionCorrelationId);
			jmsMessage.setJMSReplyTo(actionRequestor);
			//jmsMessage.setStringProperty(Node.NODE_MQ_PROP_CLUSTER, "");
			jmsMessage.setStringProperty(Node.NODE_MQ_PROP_NODE, xmlTaskAction.getNodeId());
			jmsMessage.setStringProperty(Node.NODE_MQ_PROP_ACTIONID, xmlTaskAction.getActionId());

			Marshaller marshaller = CLUSTER_JAXB_CTX.createMarshaller();
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			marshaller.marshal(new JAXBElement(new QName(CLUSTER_NAMESPACE, "TaskAction"), org.apache.ode.runtime.exec.cluster.xml.TaskAction.class,
					xmlTaskAction), bos);
			jmsMessage.writeBytes(bos.toByteArray());
			taskActionSender.send(jmsMessage);
		} catch (Exception je) {
			log.log(Level.SEVERE, "", je);
			throw new PlatformException(je);
		}
	}

	public org.apache.ode.runtime.exec.cluster.xml.Task convert(TaskImpl task) {
		org.apache.ode.runtime.exec.cluster.xml.Task xmlTask = new org.apache.ode.runtime.exec.cluster.xml.Task();
		xmlTask.setTaskId(String.valueOf(((TaskIdImpl) task.id()).taskId));
		xmlTask.setNodeId(nodeId);
		Calendar mod = Calendar.getInstance();
		mod.setTime(task.modified());
		xmlTask.setModified(mod);
		while (!msgQueue.isEmpty()) {
			xmlTask.getTaskMessages().getTaskMessage().add(msgQueue.pop());
		}
		switch (task.state()) {
		case SUBMIT:
		case START:
			Calendar start = Calendar.getInstance();
			start.setTime(task.start());
			xmlTask.setStart(start);
			break;
		case EXECUTE:
			break;
		case FINISH:

			break;
		case COMPLETE:
			Document out = task.getDOMOutput();
			if (out != null) {
				xmlTask.setExchange(new org.apache.ode.runtime.exec.cluster.xml.Task.Exchange());
				xmlTask.getExchange().setType(ExchangeType.OUTPUT);
				xmlTask.getExchange().setPayload(out.getDocumentElement());

			}
			Calendar fin = Calendar.getInstance();
			fin.setTime(task.finish());
			xmlTask.setFinished(fin);
			break;
		case CANCEL:
			fin = Calendar.getInstance();
			fin.setTime(task.finish());
			xmlTask.setFinished(fin);
			break;

		}
		return xmlTask;
	}

	public org.apache.ode.runtime.exec.cluster.xml.TaskAction convert(TaskActionExecution tae) {
		org.apache.ode.runtime.exec.cluster.xml.TaskAction xmlTaskAction = new org.apache.ode.runtime.exec.cluster.xml.TaskAction();
		xmlTaskAction.setName(tae.request.action);
		//xmlTaskAction.setComponent(tae.)
		xmlTaskAction.setActionId(tae.taskActionId);
		xmlTaskAction.setTaskId(String.valueOf(taskId.taskId));
		xmlTaskAction.setNodeId(tae.request.nodeId);
		xmlTaskAction.setName(tae.request.action);
		//xmlTaskAction.setState(value)
		xmlTaskAction.setLogLevel(org.apache.ode.runtime.exec.cluster.xml.LogLevel.valueOf(logLevel.name()));
		return xmlTaskAction;

	}

}