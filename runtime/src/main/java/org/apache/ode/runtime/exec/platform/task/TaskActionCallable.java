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
import static org.apache.ode.runtime.exec.platform.task.TaskExecutor.convertToObject;
import static org.apache.ode.spi.exec.Node.CLUSTER_NAMESPACE;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
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
import javax.xml.namespace.QName;

import org.apache.ode.runtime.exec.cluster.xml.ExchangeType;
import org.apache.ode.runtime.exec.cluster.xml.TaskAction.Exchange;
import org.apache.ode.runtime.exec.cluster.xml.TaskActionMessages;
import org.apache.ode.runtime.exec.platform.MessageImpl;
import org.apache.ode.runtime.exec.platform.NodeImpl.MessageCheck;
import org.apache.ode.runtime.exec.platform.NodeImpl.NodeId;
import org.apache.ode.runtime.exec.platform.NodeImpl.TaskCheck;
import org.apache.ode.runtime.exec.platform.task.TaskActionImpl.TaskActionIdImpl;
import org.apache.ode.spi.exec.Message.LogLevel;
import org.apache.ode.spi.exec.Node;
import org.apache.ode.spi.exec.PlatformException;
import org.apache.ode.spi.exec.task.CoordinatedTaskActionExec;
import org.apache.ode.spi.exec.task.TaskAction;
import org.apache.ode.spi.exec.task.TaskActionActivity;
import org.apache.ode.spi.exec.task.TaskActionContext;
import org.apache.ode.spi.exec.task.TaskActionDefinition;
import org.apache.ode.spi.exec.task.TaskActionExec;
import org.apache.ode.spi.exec.task.TaskActionTransaction;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class TaskActionCallable implements Callable<TaskAction.TaskActionState> {

	@NodeId
	String nodeId;

	@PersistenceUnit(unitName = "platform")
	EntityManagerFactory pmgrFactory;

	@TaskCheck
	QueueConnectionFactory taskQueueConFactory;

	@MessageCheck
	TopicConnectionFactory msgTopicConFactory;

	@MessageCheck
	Topic msgUpdateTopic;

	private QueueConnection actionUpdateQueueConnection;
	private QueueSession actionUpdateSession;

	private TopicConnection msgUpdateTopicConnection;
	private TopicSession msgUpdateSession;
	private TopicPublisher msgUpdatePublisher;

	private String correlationId;
	private Queue actionRequestor;
	private QueueSender actionRequestorSender;
	//private Queue actionUpdate;
	//private QueueReceiver actionUpdateReceiver;

	private TaskActionIdImpl id;
	private Element actionInput;

	private org.apache.ode.runtime.exec.cluster.xml.TaskCheck config;
	private JAXBContext actionJAXBContext;
	private LogLevel logLevel = LogLevel.WARNING;
	private String taskId;
	private LinkedList<org.apache.ode.runtime.exec.cluster.xml.Message> msgQueue = new LinkedList<org.apache.ode.runtime.exec.cluster.xml.Message>();
	private final Lock actionUpdateLock = new ReentrantLock();
	private final Condition actionUpdateSignal = actionUpdateLock.newCondition();
	private ConcurrentHashMap<String, TaskActionCallable> executingActions;

	EntityManager pmgr;
	TaskActionImpl taskAction;
	TaskActionActivity exec;
	TaskActionContextImpl taskActionCtx;

	Map<QName, TaskActionDefinition<?, ?>> actions;

	private static final Logger log = Logger.getLogger(TaskActionCallable.class.getName());

	public void config(Node node, org.apache.ode.runtime.exec.cluster.xml.TaskCheck config, LogLevel level, String correlationId, Queue actionRequestor,
			String taskId, TaskActionIdImpl id, Element actionInput, ConcurrentHashMap<String, TaskActionCallable> executingActions) {
		this.actions = node.getTaskActionDefinitions();
		this.config = config;
		this.logLevel = level;
		this.correlationId = correlationId;
		this.actionRequestor = actionRequestor;
		this.taskId = taskId;
		this.id = id;
		this.actionInput = actionInput;
	}

	@Override
	public TaskAction.TaskActionState call() throws PlatformException {

		try {
			pmgr = pmgrFactory.createEntityManager();
			taskAction = pmgr.find(TaskActionImpl.class, id.id());

			try {
				actionUpdateQueueConnection = taskQueueConFactory.createQueueConnection();
				msgUpdateTopicConnection = msgTopicConFactory.createTopicConnection();

				actionUpdateSession = actionUpdateQueueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
				if (actionRequestor != null) {
					actionRequestorSender = actionUpdateSession.createSender(actionRequestor);
				}
				msgUpdateSession = msgUpdateTopicConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
				msgUpdatePublisher = msgUpdateSession.createPublisher(msgUpdateTopic);

				actionUpdateQueueConnection.start();
				msgUpdateTopicConnection.start();

				init();
				execute();
				destroy();

			} catch (JMSException je) {
				log.log(Level.SEVERE, "", je);
				fail(je.getMessage(), false);
				//} //catch (PlatformException pe) {
				//	throw pe;
			} finally {
				try {
					actionUpdateQueueConnection.close();
				} catch (JMSException e) { //don't care about JMS errors on closure
				}
				try {
					msgUpdateTopicConnection.close();
				} catch (JMSException e) { //don't care about JMS errors on closure
				}
			}

		} finally {
			executingActions.remove(this);
			pmgr.close();

		}
		return taskAction.state();

	}

	public void init() throws PlatformException {
		TaskActionDefinition def = actions.get(taskAction.name());
		if (def == null) {
			fail(String.format("Unsupported TaskAction %s", taskAction.name().toString()), false);
		}
		actionJAXBContext = def.jaxbContext();
		taskActionCtx = new TaskActionContextImpl();
		exec = (TaskActionActivity) def.actionExec().get();

		/*if (exec instanceof CoordinatedTaskActionExec || exec instanceof TaskActionTransaction) {
			try {
				actionUpdate = actionUpdateSession.createTemporaryQueue();
				actionUpdateReceiver = actionUpdateSession.createReceiver(actionUpdate);
			} catch (JMSException je) {
				log.log(Level.SEVERE, "", je);
				fail(je.getMessage(), false);
			}
		}*/
		taskAction.setState(TaskAction.TaskActionState.START);
		taskAction.setStart(new Date());
		updateAction();
		try {
			exec.start(taskActionCtx, convertToObject(actionInput, locateTarget(exec.getClass(), ExchangeType.INPUT), actionJAXBContext));
		} catch (Throwable t) {
			fail(t.getMessage(), true);
		}
		if (taskActionCtx.failed) {
			fail(null, true);
		}
	}

	public void execute() throws PlatformException {
		taskAction.setState(TaskAction.TaskActionState.EXECUTE);
		updateAction();
		if (exec instanceof TaskActionExec) {
			try {
				((TaskActionExec) exec).execute();
			} catch (Throwable t) {
				fail(t.getMessage(), true);
			}
		} else if (exec instanceof CoordinatedTaskActionExec) {
			CoordinatedTaskActionExec cexec = (CoordinatedTaskActionExec) exec;
			while (!taskActionCtx.failed && TaskAction.TaskActionState.EXECUTE == taskAction.state()) {
				refresh(config.getActionCoordinationTimeout());
				try {
					Object cresult = cexec.execute(convertToObject(taskAction.getCoordinationInput(),
							locateTarget(exec.getClass(), ExchangeType.COORDINATE_INPUT), actionJAXBContext));
					taskAction.setCoordinationOutput(convertToDocument(cresult, actionJAXBContext));
					updateAction();
				} catch (Throwable t) {
					fail(t.getMessage(), true);
				}
			}
		}
		if (taskActionCtx.failed) {
			fail(null, true);
		}
	}

	public void destroy() throws PlatformException {
		taskAction.setState(TaskAction.TaskActionState.FINISH);
		updateAction();
		try {
			Object result = exec.finish();

			if (taskActionCtx.failed) {
				fail(null, true);
			}
			taskAction.setDOMOutput(convertToDocument(result, actionJAXBContext));
		} catch (Throwable t) {
			fail(t.getMessage(), true);
		}
		if (exec instanceof TaskActionTransaction) {
			taskAction.setState(TaskAction.TaskActionState.PENDING);
			updateAction();
			refresh(config.getActionTransactionTimeout());
			((TaskActionTransaction) exec).complete();
			if (taskAction.state() == TaskAction.TaskActionState.COMMIT) {
				taskAction.setState(TaskAction.TaskActionState.COMPLETE);
			}
			taskAction.setFinish(new Date());
			updateAction();

		} else {
			taskAction.setState(TaskAction.TaskActionState.COMPLETE);
			taskAction.setFinish(new Date());
			updateAction();
		}
	}

	public void refresh(long timeout) throws PlatformException {
		try {
			actionUpdateLock.lock();
			if (!actionUpdateSignal.await(config.getActionCoordinationTimeout(), TimeUnit.MILLISECONDS)) {
				fail("refresh timed out", true);
			}
		} catch (InterruptedException e) {
			fail(String.format("refresh interupted", e.getMessage()), true);
		} finally {
			actionUpdateLock.unlock();
		}
		/*try {
			BytesMessage message = (BytesMessage) actionUpdateReceiver.receive(timeout);

			if (message != null) {
				if (correlationId.equals(message.getJMSCorrelationID())) {
					byte[] payload = new byte[(int) message.getBodyLength()];
					message.readBytes(payload);
					Unmarshaller umarshaller = CLUSTER_JAXB_CTX.createUnmarshaller();
					JAXBElement<org.apache.ode.runtime.exec.cluster.xml.TaskAction> element = umarshaller.unmarshal(new StreamSource(new ByteArrayInputStream(
							payload)), org.apache.ode.runtime.exec.cluster.xml.TaskAction.class);
					org.apache.ode.runtime.exec.cluster.xml.TaskAction xmlTaskAction = element.getValue();

					taskAction.setState(TaskActionState.valueOf(xmlTaskAction.getState().value()));
					taskAction.setCoordinationInput(convertToDocument((Element) xmlTaskAction.getCoordinationOutput()));
					//message = (BytesMessage) tae.actionRequestorReceiver.receiveNoWait();
				} else {
					log.log(Level.WARNING, String.format("Received message without expected correlationId %s :%s", correlationId, message));
				}
			}
		} catch (Exception e) {
			fail(e.getMessage(), false);
		}*/

		/*
		try {
			actionUpdateLock.lock();
			taskAction.setState(TaskAction.TaskActionState.valueOf(xmlAction.getState().value()));
			taskAction.setCoordinationInput(convertToDocument((Element) xmlAction.getCoordinationInput()));
			taskAction.setCoordinationOutput(null);
			actionUpdateSignal.signal();
		} finally {
			actionUpdateLock.unlock();
		}
		*/
	}

	public void externalUpdate(org.apache.ode.runtime.exec.cluster.xml.TaskAction xmlAction) {
		try {
			actionUpdateLock.lock();
			taskAction.setState(TaskAction.TaskActionState.valueOf(xmlAction.getState().value()));
			taskAction.setCoordinationInput(convertToDocument(xmlAction.getExchange().getPayload()));
			taskAction.setCoordinationOutput(null);
			actionUpdateSignal.signal();
		} finally {
			actionUpdateLock.unlock();
		}
	}

	public void updateAction() {
		pmgr.getTransaction().begin();
		try {
			pmgr.merge(taskAction);
			pmgr.getTransaction().commit();
		} catch (PersistenceException pe) {
			log.log(Level.SEVERE, "", pe);
		}
		if (actionRequestorSender != null) {
			try {
				org.apache.ode.runtime.exec.cluster.xml.TaskAction xmlAction = convert(taskAction);
				BytesMessage jmsMessage = actionUpdateSession.createBytesMessage();
				jmsMessage.setJMSCorrelationID(correlationId);
				//jmsMessage.setJMSReplyTo(actionUpdate);
				Marshaller marshaller = CLUSTER_JAXB_CTX.createMarshaller();
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				marshaller.marshal(new JAXBElement(new QName(CLUSTER_NAMESPACE, "TaskAction"), org.apache.ode.runtime.exec.cluster.xml.TaskAction.class,
						xmlAction), bos);
				jmsMessage.writeBytes(bos.toByteArray());
				actionRequestorSender.send(jmsMessage);
			} catch (Exception je) {
				log.log(Level.SEVERE, "", je);
			}
		}
	}

	public void updateLog(LogLevel level, int code, String message) {
		MessageImpl m = new MessageImpl();
		pmgr.getTransaction().begin();
		try {
			m.setLevel(level.toString());
			m.setCode(code);
			m.setMessage(message);
			m.setTimestamp(Calendar.getInstance().getTime());
			taskAction.messages().add(m);
			pmgr.merge(taskAction);
			pmgr.getTransaction().commit();
		} catch (PersistenceException pe) {
			log.log(Level.SEVERE, "", pe);
		}
		log(m, logLevel, msgQueue, msgUpdateSession, correlationId, msgUpdatePublisher);
	}

	public class TaskActionContextImpl implements TaskActionContext {
		boolean failed = false;;

		@Override
		public TaskAction.TaskActionId id() {
			return taskAction.id();
		}

		@Override
		public QName name() {
			return taskAction.name();
		}

		@Override
		public void log(LogLevel level, int code, String message) {
			updateLog(level, code, message);
		}

		@Override
		public TaskAction.TaskActionState getState() {
			return taskAction.state();
		}

		@Override
		public void setFailed() {
			failed = true;
		}

	}

	public void fail(String msg, boolean rollback) throws PlatformException {
		if (msg != null) {
			updateLog(LogLevel.ERROR, 0, msg);
		}
		if (rollback && exec instanceof TaskActionTransaction) {
			taskAction.setState(TaskAction.TaskActionState.ROLLBACK);
			((TaskActionTransaction) exec).complete();
		}
		taskAction.setState(TaskAction.TaskActionState.FAILED);
		taskAction.setFinish(new Date());
		updateAction();

		throw new PlatformException(msg);

	}

	public org.apache.ode.runtime.exec.cluster.xml.TaskAction convert(TaskActionImpl action) {
		org.apache.ode.runtime.exec.cluster.xml.TaskAction xmlAction = new org.apache.ode.runtime.exec.cluster.xml.TaskAction();
		xmlAction.setName(action.name());
		//xmlAction.setComponent(action.component());
		xmlAction.setActionId(String.valueOf(((TaskActionIdImpl) action.id()).taskActionId));
		xmlAction.setTaskId(taskId);
		xmlAction.setNodeId(nodeId);
		xmlAction.setState(org.apache.ode.runtime.exec.cluster.xml.TaskActionState.valueOf(action.state().name()));

		Calendar mod = Calendar.getInstance();
		mod.setTime(action.modified());
		xmlAction.setModified(mod);
		xmlAction.setTaskActionMessages(new TaskActionMessages());
		while (!msgQueue.isEmpty()) {
			xmlAction.getTaskActionMessages().getTaskActionMessage().add(msgQueue.pop());
		}

		switch (action.state()) {
		case SUBMIT:
		case START:
			Calendar start = Calendar.getInstance();
			start.setTime(action.start());
			xmlAction.setStart(start);
			break;
		case EXECUTE:
			Document out = action.getCoordinationOutput();
			if (out != null) {
				xmlAction.setExchange(new Exchange());
				xmlAction.getExchange().setType(ExchangeType.COORDINATE_OUTPUT);
				xmlAction.getExchange().setPayload(out.getDocumentElement());
			}
			break;
		case FINISH:
			break;
		case PENDING:
			out = action.getDOMOutput();
			if (out != null) {
				xmlAction.setExchange(new Exchange());
				xmlAction.getExchange().setType(ExchangeType.OUTPUT);
				xmlAction.getExchange().setPayload(out.getDocumentElement());
			}
			break;
		case ROLLBACK:
		case COMMIT:
		case COMPLETE:
			Calendar fin = Calendar.getInstance();
			fin.setTime(action.finish());
			xmlAction.setFinished(fin);
			if (!(exec instanceof CoordinatedTaskActionExec)) {
				out = action.getDOMOutput();
				if (out != null) {
					xmlAction.setExchange(new Exchange());
					xmlAction.getExchange().setType(ExchangeType.OUTPUT);
					xmlAction.getExchange().setPayload(out.getDocumentElement());
				}
			}
			break;
		case FAILED:
			fin = Calendar.getInstance();
			fin.setTime(action.finish());
			xmlAction.setFinished(fin);
			break;

		}
		return xmlAction;
	}

	public static Class<?> locateTarget(Class<?> clazz, ExchangeType target) throws PlatformException {
		Class<?> targetClass = null;
		for (Type t : clazz.getGenericInterfaces()) {
			if (t instanceof ParameterizedType) {
				ParameterizedType pt = (ParameterizedType) t;
				if (TaskActionActivity.class.equals(pt.getRawType()) || TaskActionExec.class.equals(pt.getRawType())) {
					if (target == ExchangeType.INPUT) {
						targetClass = (Class<?>) pt.getActualTypeArguments()[0];
						break;
					} else if (target == ExchangeType.OUTPUT) {
						targetClass = (Class<?>) pt.getActualTypeArguments()[1];
						break;
					}
				} else if (CoordinatedTaskActionExec.class.equals(pt.getRawType())) {
					if (target == ExchangeType.COORDINATE_INPUT) {
						targetClass = (Class<?>) pt.getActualTypeArguments()[0];
						break;
					} else if (target == ExchangeType.COORDINATE_OUTPUT) {
						targetClass = (Class<?>) pt.getActualTypeArguments()[1];
						break;
					}
				}
			}
		}

		if (targetClass == null) {
			log.severe(String.format("Unable to convert target %s on class %s", target.name(), clazz.getName()));
			throw new PlatformException(String.format("Unable to convert target %s on class %s", target.name(), clazz.getName()));
		}
		return targetClass;
	}
}