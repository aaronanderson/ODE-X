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
import static org.apache.ode.runtime.exec.platform.NodeImpl.PLATFORM_JAXB_CTX;
import static org.apache.ode.runtime.exec.platform.task.TaskExecutor.convertObject;
import static org.apache.ode.runtime.exec.platform.task.TaskExecutor.binderDocument;
import static org.apache.ode.runtime.exec.platform.task.TaskExecutor.convertXML;
import static org.apache.ode.runtime.exec.platform.task.TaskExecutor.newDocument;
import static org.apache.ode.spi.exec.Node.CLUSTER_NAMESPACE;
import static org.apache.ode.spi.exec.Node.NODE_MQ_CORRELATIONID_ACTION;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import org.apache.ode.runtime.exec.platform.MessageImpl;
import org.apache.ode.runtime.exec.platform.NodeImpl.MessageCheck;
import org.apache.ode.runtime.exec.platform.NodeImpl.NodeId;
import org.apache.ode.runtime.exec.platform.NodeImpl.TaskCheck;
import org.apache.ode.runtime.exec.platform.target.TargetImpl;
import org.apache.ode.runtime.exec.platform.task.TaskCallable.TaskResult;
import org.apache.ode.runtime.exec.platform.task.TaskExecutor.IOType;
import org.apache.ode.runtime.exec.platform.task.TaskExecutor.JAXBConversion;
import org.apache.ode.runtime.exec.platform.task.TaskImpl.TaskIdImpl;
import org.apache.ode.spi.exec.Message.LogLevel;
import org.apache.ode.spi.exec.Node;
import org.apache.ode.spi.exec.PlatformException;
import org.apache.ode.spi.exec.platform.xml.ExchangeType;
import org.apache.ode.spi.exec.platform.xml.TaskAction.Exchange;
import org.apache.ode.spi.exec.target.Target;
import org.apache.ode.spi.exec.task.CoordinatedTaskActionCoordinator;
import org.apache.ode.spi.exec.task.IOBuilder;
import org.apache.ode.spi.exec.task.Input;
import org.apache.ode.spi.exec.task.Output;
import org.apache.ode.spi.exec.task.Request;
import org.apache.ode.spi.exec.task.Response;
import org.apache.ode.spi.exec.task.Task.TaskId;
import org.apache.ode.spi.exec.task.Task.TaskState;
import org.apache.ode.spi.exec.task.TaskAction.TaskActionState;
import org.apache.ode.spi.exec.task.TaskActionCoordinator;
import org.apache.ode.spi.exec.task.TaskActionDefinition;
import org.apache.ode.spi.exec.task.TaskCallback;
import org.apache.ode.spi.exec.task.TaskContext;
import org.apache.ode.spi.exec.task.TaskDefinition;
import org.apache.ode.spi.exec.task.TaskDefinition.TaskActionCoordinatorDefinition;
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
	private IOBuilder taskIOBuilder;
	private Element taskInput;
	private Element taskOutput;

	private JAXBContext taskJaxbContext;

	private Target[] targets;
	private EntityManager pmgr;
	private TaskImpl task;
	private LogLevel logLevel;

	private LinkedList<org.apache.ode.spi.exec.platform.xml.Message> msgQueue = new LinkedList<org.apache.ode.spi.exec.platform.xml.Message>();

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
		IOBuilder actionIOBuilder;
		Response response;
		Request request;
		Set<TaskActionExecution> prevDependencies = new HashSet<TaskActionExecution>();
		//Set<TaskActionExecution> nextDependencies = new HashSet<TaskActionExecution>();
		private Queue actionRequestor;
		private String actionCorrelationId;
		private QueueReceiver actionRequestorReceiver;

	}

	public class TaskCoordinatorExecution {
		QName name;
		Set<QName> dependendencies;
		TaskActionCoordinator coordinator;
		Set<TaskActionExecution> requests = new HashSet<TaskActionExecution>();
		boolean initialized = false;
		boolean finialized = false;
		int initOrder = -1;
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
				Map<QName, Set<TaskActionExecution>> actionExecutions = new HashMap<QName, Set<TaskActionExecution>>();
				try {
					init(coordinators, actionExecutions);
					do {
						for (Set<TaskActionExecution> taes : actionExecutions.values()) {
							for (TaskActionExecution tae : taes) {
								refreshAction(tae);
								executeAction(tae);
							}
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

		@Override
		public String localNodeId() {
			return nodeId;
		}

		@Override
		public <I> Request<I> newRequest(QName action, String nodeId, I input) {
			TaskActionDefinition def = actions.get(action);
			return new Request<I>(action, nodeId, new Input(input, def.jaxbContext().createBinder(org.w3c.dom.Node.class)), false);
		}
	}

	public void init(Map<QName, TaskCoordinatorExecution> coordinators, Map<QName, Set<TaskActionExecution>> actionExecutions) throws PlatformException {

		TaskDefinition<?, ?> def = tasks.get(task.name());
		if (def == null) {
			taskLogIt(LogLevel.ERROR, 0, String.format("No TaskDefinition defined for task %s", task.name()), true);
		}

		task.setState(TaskState.START);
		task.setStart(new Date());
		updateTask();
		try {
			taskJaxbContext = def.jaxbContext();
		} catch (JAXBException je) {
			throw new PlatformException(je);
		}
		taskIOBuilder = def.ioFactory();
		for (TaskActionCoordinatorDefinition coordinatorDef : def.coordinators()) {
			TaskCoordinatorExecution taskCordExec = new TaskCoordinatorExecution();
			taskCordExec.name = coordinatorDef.name;
			taskCordExec.dependendencies = coordinatorDef.dependencies;
			taskCordExec.coordinator = (TaskActionCoordinator) coordinatorDef.coordinator.get();
			taskCordExec.initialized = false;
			taskCordExec.finialized = false;
			coordinators.put(coordinatorDef.name, taskCordExec);
		}
		if (coordinators.size() == 0) {
			taskLogIt(LogLevel.ERROR, 0, String.format("No TaskCoordinators registered for task %s", task.name()), true);
		}
		for (TaskCoordinatorExecution coordExec : (coordinators.values())) {
			for (QName dep : (Set<QName>) coordExec.dependendencies) {
				if (!coordinators.containsKey(dep)) {
					taskLogIt(LogLevel.ERROR, 0, String.format("TaskCoordinator %s dependency %s not registered", coordExec.name, dep), true);
				}
			}
		}

		JAXBConversion value = convertXML(IOType.INPUT, taskInput, taskIOBuilder, taskJaxbContext);
		Input taskRequest = new Input(value.jValue, value.binder);
		int initOrder = 0;
		boolean finished = false;
		while (!finished) {
			finished = true;
			boolean modified = false;
			Set<QName> remaining = new HashSet<QName>();
			for (TaskCoordinatorExecution coordExec : (coordinators.values())) {
				boolean dependenciesCompleted = true;
				for (QName depExecName : (Set<QName>) coordExec.dependendencies) {
					if (!coordinators.get(depExecName).initialized) {
						dependenciesCompleted = false;
						remaining.add(depExecName);
						break;
					}
				}
				if (dependenciesCompleted) {
					coordExec.initOrder = initOrder++;
					Set<Request<?>> requests = coordExec.coordinator.init(taskContext, taskRequest, callback, targets);
					coordExec.initialized=true;

					for (Request<?> canidate : requests) {
						TaskActionDefinition tad = actions.get(canidate.name);
						if (tad == null) {
							taskLogIt(LogLevel.ERROR, 0, String.format("Unsupported TaskAction %s", canidate.name.toString()), true);
						}
						if (actionExecutions.containsKey(canidate.name)) {
							for (TaskActionExecution tae : actionExecutions.get(canidate.name)) {
								if (canidate.nodeId.equals(tae.request.nodeId)) {
									taskLogIt(LogLevel.ERROR, 0, String.format("Duplicate TaskAction %s", canidate.name.toString()), true);
								}
							}
						}
						TaskActionExecution tae = new TaskActionExecution();
						tae.owner = coordExec;
						tae.request = canidate;
						tae.actionJaxbContext = tad.jaxbContext();
						tae.actionIOBuilder = tad.ioBuilder();
						try {
							tae.actionRequestor = taskUpdateSession.createTemporaryQueue();
							tae.actionRequestorReceiver = taskUpdateSession.createReceiver(tae.actionRequestor);
							tae.actionCorrelationId = String.format(NODE_MQ_CORRELATIONID_ACTION, System.currentTimeMillis());
						} catch (JMSException e) {
							taskLogIt(LogLevel.ERROR, 0, e.getMessage(), true);
						}
						coordExec.requests.add(tae);
						Set<TaskActionExecution> taes = actionExecutions.get(canidate.name);
						if (taes == null) {
							taes = new HashSet<TaskActionExecution>();
							actionExecutions.put(canidate.name, taes);
						}
						taes.add(tae);
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
			for (Set<TaskActionExecution> actionExecs : actionExecutions.values()) {
				for (TaskActionExecution actionExec : actionExecs) {
					if (resolved.get(actionExec.request.name) != null) {
						if (resolved.get(actionExec.request.name)) {
							continue;
						}
						boolean dependenciesResolved = true;
						for (QName dep : actions.get(actionExec.request.name).dependencies()) {
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
							resolved.put(actionExec.request.name, true);
							modified = true;
						}
					} else {
						resolved.put(actionExec.request.name, false);
						finished = false;
						modified = true;
						for (QName dep : actions.get(actionExec.request.name).dependencies()) {
							Set<TaskActionExecution> depActionExec = actionExecutions.get(dep);
							if (depActionExec == null) {
								taskLogIt(LogLevel.ERROR, 0, String.format("TaskAction %s dependency %s not submitted for execution by a task coordinator",
										actionExec.request.name, dep), true);
							}
							actionExec.prevDependencies.addAll(depActionExec);
							//depActionExec.nextDependencies.add(actionExec);
						}
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
			task.setDOMInput(newDocument(taskInput));
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
			taskLogIt(LogLevel.ERROR, 0, String.format("TaskAction timed out: %s %s", tae.request.name, tae.request.nodeId), false);
			//tae.state=TaskActionState.FAILED;
		}
		org.apache.ode.spi.exec.platform.xml.TaskAction xmlTaskAction = convert(tae);

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
					Set<Response> dependencies = new HashSet<Response>();
					for (TaskActionExecution dep : tae.prevDependencies) {
						dependencies.add(dep.response);
					}
					if (dependencies.size() > 0) {
						tae.owner.coordinator.update(tae.request, dependencies);
						if (tae.request.skipped) {
							tae.state = TaskActionState.SKIPPED;
							return;
						}
					}
					xmlTaskAction.setState(org.apache.ode.spi.exec.platform.xml.TaskActionState.SUBMIT);
					if (tae.request.input != null) {
						xmlTaskAction.setExchange(new Exchange());
						xmlTaskAction.getExchange().setType(ExchangeType.INPUT);
						JAXBConversion value = convertObject(IOType.INPUT, tae.request.input.value, tae.actionIOBuilder, tae.actionJaxbContext);
						xmlTaskAction.getExchange().setPayload(value.xValue.getDocumentElement());
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
					TaskActionDefinition coordAction = actions.get(xmlTaskAction.getName());
					if (coordAction == null) {

					}
					JAXBConversion value = convertXML(IOType.OUTPUT, tae.actionCoordinationOutput, coordAction.ioBuilder(), tae.actionJaxbContext);
					Response cres = new Response(coordAction.action(), xmlTaskAction.getNodeId(), new Output(value.jValue, value.binder), true);
					Request creq = ((CoordinatedTaskActionCoordinator) tae.owner).coordinate(cres);
					tae.actionCoordinationOutput = null;
					xmlTaskAction.setState(org.apache.ode.spi.exec.platform.xml.TaskActionState.EXECUTE);
					xmlTaskAction.setExchange(new Exchange());
					xmlTaskAction.getExchange().setType(ExchangeType.COORDINATE_INPUT);
					xmlTaskAction.getExchange().setPayload(value.xValue.getDocumentElement());
					updateTaskAction(xmlTaskAction, tae.actionCorrelationId, tae.actionRequestor);
				}
				break;
			case PENDING:
				JAXBConversion value = convertXML(IOType.OUTPUT, tae.actionOutput, tae.actionIOBuilder, tae.actionJaxbContext);
				tae.response = new Response(tae.request.name, tae.request.nodeId, new Output(value.jValue, value.binder), true);
				break;
			/*case FINISH:
			break;
			
			case COMMIT:
			break;
			case ROLLBACK:
			break;*/
			case COMPLETE:
				if (tae.previousState != TaskActionState.PENDING) {
					value = convertXML(IOType.OUTPUT, tae.actionOutput, tae.actionIOBuilder, tae.actionJaxbContext);
					tae.response = new Response(tae.request.name, tae.request.nodeId, new Output(value.jValue, value.binder), true);
				}
				break;

			case SKIPPED:
				tae.response = new Response(tae.request.name, tae.request.nodeId, null, false);
				pmgr.getTransaction().begin();
				try {
					TaskActionImpl taskAction = pmgr.find(TaskActionImpl.class, Long.valueOf(xmlTaskAction.getActionId()));
					if (taskAction != null) {
						taskAction.setState(TaskActionState.SKIPPED);
						Date now = new Date();
						taskAction.setStart(now);
						taskAction.setFinish(now);
						pmgr.merge(taskAction);
						pmgr.getTransaction().commit();
					} else {
						log.warning(String.format("Unable to find TaskAction %s for Task %s", xmlTaskAction.getActionId(), taskId));
						pmgr.getTransaction().rollback();
					}
				} catch (PersistenceException pe) {
					log.log(Level.SEVERE, "", pe);
				}
				break;

			case FAILED:
				value = convertXML(IOType.OUTPUT, tae.actionOutput, tae.actionIOBuilder, tae.actionJaxbContext);
				tae.response = new Response(tae.request.name, tae.request.nodeId, new Output(value.jValue, value.binder), false);
				break;

			}
		} catch (PlatformException pe) {
			tae.previousState = tae.state;
			tae.state = TaskActionState.FAILED;
			throw pe;
		}

	}

	public boolean complete(Collection<Set<TaskActionExecution>> actionExecutions) throws PlatformException {

		boolean finished = true;
		for (Set<TaskActionExecution> taes : actionExecutions) {
			for (TaskActionExecution tae : taes) {
				if (tae.state != TaskActionState.PENDING) {
					finished &= !isExecuting(tae);
				}
			}
		}
		return finished;
	}

	public Document destroy(Map<QName, TaskCoordinatorExecution> coordinators, Map<QName, Set<TaskActionExecution>> actionExecutions) throws PlatformException {

		Document doc = null;

		task.setState(TaskState.FINISH);
		updateTask();

		List<TaskCoordinatorExecution> destroyList = new ArrayList<TaskCoordinatorExecution>(coordinators.values());
		Collections.sort(destroyList, new Comparator<TaskCoordinatorExecution>() {

			@Override
			public int compare(TaskCoordinatorExecution arg0, TaskCoordinatorExecution arg1) {
				//we want to destroy in backwards order
				return arg0.initOrder < arg1.initOrder ? 1 : arg0.initOrder > arg1.initOrder ? -1 : 0;
			}
		});
		JAXBConversion value = convertObject(IOType.OUTPUT, null, taskIOBuilder, taskJaxbContext);
		Output response = new Output(value.jValue, value.binder);
		for (TaskCoordinatorExecution coordExec : destroyList) {
			if (!coordExec.initialized) {
				continue;
			}
			//Object output = convertToObject(doc, locateTaskTarget(coordExec.coordinator.getClass(), ExchangeType.OUTPUT), coordExec.taskJaxbContext);
			Set<Response> responses = new HashSet<Response>();
			for (TaskActionExecution tae : coordExec.requests) {
				if (tae.response != null) {
					responses.add(tae.response);
				} else {
					Response res = new Response(tae.request.name, tae.request.nodeId, null, false);
					responses.add(res);
				}
			}
			coordExec.coordinator.finish(responses, response);
		}
		doc = binderDocument(IOType.OUTPUT, response.value, value.xValue, taskIOBuilder, value.binder);

		Set<TaskActionExecution> pending = new HashSet<TaskActionExecution>();
		for (Set<TaskActionExecution> taes : actionExecutions.values()) {
			for (TaskActionExecution tae : taes) {
				if (tae.state == TaskActionState.PENDING) {
					pending.add(tae);
					org.apache.ode.spi.exec.platform.xml.TaskAction xmlTaskAction = convert(tae);
					xmlTaskAction.setState(taskContext.failed ? org.apache.ode.spi.exec.platform.xml.TaskActionState.ROLLBACK
							: org.apache.ode.spi.exec.platform.xml.TaskActionState.COMMIT);
					updateTaskAction(xmlTaskAction, tae.actionCorrelationId, tae.actionRequestor);
				}
			}
		}

		for (TaskActionExecution tae : pending) {
			refreshAction(tae, true);
			if (tae.state == TaskActionState.PENDING) {
				//this should not happen
			}
		}

		for (Set<TaskActionExecution> taes : actionExecutions.values()) {
			for (TaskActionExecution tae : taes) {
				try {
					if (tae.actionRequestorReceiver != null) {
						tae.actionRequestorReceiver.close();
					}
				} catch (JMSException e) {//don't care on close
				}
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
						Unmarshaller umarshaller = PLATFORM_JAXB_CTX.createUnmarshaller();
						JAXBElement<org.apache.ode.spi.exec.platform.xml.TaskAction> element = umarshaller.unmarshal(new StreamSource(new ByteArrayInputStream(
								payload)), org.apache.ode.spi.exec.platform.xml.TaskAction.class);
						org.apache.ode.spi.exec.platform.xml.TaskAction xmlTaskAction = element.getValue();
						tae.taskActionId = xmlTaskAction.getActionId();
						tae.previousState = tae.state;
						tae.state = TaskActionState.valueOf(xmlTaskAction.getState().value());
						tae.lastUpdate = xmlTaskAction.getModified().getTimeInMillis();
						for (org.apache.ode.spi.exec.platform.xml.Message msg : xmlTaskAction.getTaskActionMessages().getTaskActionMessage()) {
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
		return tae.state == TaskActionState.SKIPPED || tae.state == TaskActionState.COMPLETE || tae.state == TaskActionState.FAILED ? false : true;
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
				org.apache.ode.spi.exec.platform.xml.Task xmlTask = convert(task);
				BytesMessage jmsMessage = taskUpdateSession.createBytesMessage();
				Marshaller marshaller = PLATFORM_JAXB_CTX.createMarshaller();
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				marshaller.marshal(new JAXBElement(new QName(CLUSTER_NAMESPACE, "Task"), org.apache.ode.spi.exec.platform.xml.Task.class, xmlTask), bos);
				jmsMessage.writeBytes(bos.toByteArray());
				taskRequestorSender.send(jmsMessage);
			} catch (Exception je) {
				log.log(Level.SEVERE, "", je);
				throw new PlatformException(je);
			}
		}
	}

	public void updateTaskAction(org.apache.ode.spi.exec.platform.xml.TaskAction xmlTaskAction, String actionCorrelationId, Queue actionRequestor)
			throws PlatformException {
		try {
			BytesMessage jmsMessage = taskUpdateSession.createBytesMessage();
			jmsMessage.setJMSCorrelationID(actionCorrelationId);
			jmsMessage.setJMSReplyTo(actionRequestor);
			//jmsMessage.setStringProperty(Node.NODE_MQ_PROP_CLUSTER, "");
			jmsMessage.setStringProperty(Node.NODE_MQ_PROP_NODE, xmlTaskAction.getNodeId());
			jmsMessage.setStringProperty(Node.NODE_MQ_PROP_ACTIONID, xmlTaskAction.getActionId());

			Marshaller marshaller = PLATFORM_JAXB_CTX.createMarshaller();
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			marshaller.marshal(
					new JAXBElement(new QName(CLUSTER_NAMESPACE, "TaskAction"), org.apache.ode.spi.exec.platform.xml.TaskAction.class, xmlTaskAction), bos);
			jmsMessage.writeBytes(bos.toByteArray());
			taskActionSender.send(jmsMessage);
		} catch (Exception je) {
			log.log(Level.SEVERE, "", je);
			throw new PlatformException(je);
		}
	}

	public org.apache.ode.spi.exec.platform.xml.Task convert(TaskImpl task) {
		org.apache.ode.spi.exec.platform.xml.Task xmlTask = new org.apache.ode.spi.exec.platform.xml.Task();
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
				xmlTask.setExchange(new org.apache.ode.spi.exec.platform.xml.Task.Exchange());
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

	public org.apache.ode.spi.exec.platform.xml.TaskAction convert(TaskActionExecution tae) {
		org.apache.ode.spi.exec.platform.xml.TaskAction xmlTaskAction = new org.apache.ode.spi.exec.platform.xml.TaskAction();
		xmlTaskAction.setName(tae.request.name);
		//xmlTaskAction.setComponent(tae.)
		xmlTaskAction.setActionId(tae.taskActionId);
		xmlTaskAction.setTaskId(String.valueOf(taskId.taskId));
		xmlTaskAction.setNodeId(tae.request.nodeId);
		xmlTaskAction.setName(tae.request.name);
		//xmlTaskAction.setState(value)
		xmlTaskAction.setLogLevel(org.apache.ode.spi.exec.platform.xml.LogLevel.valueOf(logLevel.name()));
		return xmlTaskAction;

	}

	/*
		public <TI> TaskRequest<TI> buildTaskRequest() throws PlatformException {
			try {
				Binder<org.w3c.dom.Node> binder = taskJaxbContext.createBinder(org.w3c.dom.Node.class);
				TI input = (TI) binder.unmarshal(taskInput);
				TaskRequest<TI> request = new TaskRequest<TI>(input);
				request.binder = binder;
				return request;
			} catch (JAXBException je) {
				taskLogIt(LogLevel.ERROR, 0, je.getMessage(), true);
				throw new PlatformException(je);
			}
		}

		public <TO> TaskResponse<TO> buildTaskResponse() throws PlatformException {
			try {
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				dbf.setNamespaceAware(true);
				Document doc = dbf.newDocumentBuilder().newDocument();
				Binder<org.w3c.dom.Node> binder = taskJaxbContext.createBinder(org.w3c.dom.Node.class);
				TO output = (TO) taskIOFactory.newOutput();
				taskOutput = doc.getDocumentElement();
				binder.marshal(output, taskOutput);
				return new TaskResponse<TO>(output);
			} catch (Exception je) {
				taskLogIt(LogLevel.ERROR, 0, je.getMessage(), true);
				throw new PlatformException(je);
			}
		}

		public Element buildActionRequestElement(TaskActionExecution tae) throws PlatformException {
			//tae.request.input, tae.actionJaxbContext
			try {
				if (tae.request.binder == null) {
					DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
					dbf.setNamespaceAware(true);
					Document doc = dbf.newDocumentBuilder().newDocument();
					Binder<org.w3c.dom.Node> binder = tae.actionJaxbContext.createBinder(org.w3c.dom.Node.class);
					binder.marshal(tae.request.input, doc.getDocumentElement());
				}
				return (Element) tae.request.binder.getXMLNode(tae.request.input);
			} catch (Exception je) {
				taskLogIt(LogLevel.ERROR, 0, je.getMessage(), true);
				throw new PlatformException(je);
			}

		}
		/*
		public Element buildCoordRequestElement(TaskActionExecution tae) throws PlatformException {
			//tae.request.input, tae.actionJaxbContext
			try {
				if (tae.request.binder == null) {
					DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
					dbf.setNamespaceAware(true);
					Document doc = dbf.newDocumentBuilder().newDocument();
					Binder<org.w3c.dom.Node> binder = tae.actionJaxbContext.createBinder(org.w3c.dom.Node.class);
					binder.marshal(tae.coordRequest.input, doc.getDocumentElement());
				}
				return (Element) tae.request.binder.getXMLNode(tae.request.input);
			} catch (Exception je) {
				taskLogIt(LogLevel.ERROR, 0, je.getMessage(), true);
				throw new PlatformException(je);
			}

		}

		public Element buildActionResponseElement(TaskActionExecution tae) {
			//tae.request.input, tae.actionJaxbContext
		}

		public TaskActionRequest buildRequest(TaskActionExecution tae) {
			//tae.request.input, tae.actionJaxbContext
		}

		/*public TaskActionResponse buildResponse(TaskActionExecution tae, boolean success) {
			//Object output = tae.actionOutput != null ? convertToObject(tae.actionOutput, tae.actionJaxbContext) : null;
			//tae.request.input, tae.actionJaxbContext
		}

		public TaskActionCoordinationRequest buildCoordinationRequest(TaskActionExecution tae) {
			//tae.request.input, tae.actionJaxbContext
		}

		public TaskActionCoordinationResponse buildCoordinationResponse(TaskActionExecution tae) {
			//tae.request.input, tae.actionJaxbContext
		}*/

}