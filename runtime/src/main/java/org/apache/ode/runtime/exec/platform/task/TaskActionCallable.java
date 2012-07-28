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

import static org.apache.ode.runtime.exec.platform.NodeImpl.CLUSTER_JAXB_CTX;
import static org.apache.ode.runtime.exec.platform.task.TaskExecutor.convertToDocument;
import static org.apache.ode.runtime.exec.platform.task.TaskExecutor.convertToObject;
import static org.apache.ode.spi.exec.Node.CLUSTER_NAMESPACE;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.Callable;
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
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import org.apache.ode.runtime.exec.platform.MessageImpl;
import org.apache.ode.runtime.exec.platform.NodeImpl.MessageCheck;
import org.apache.ode.runtime.exec.platform.NodeImpl.TaskCheck;
import org.apache.ode.runtime.exec.platform.task.TaskActionImpl.TaskActionIdImpl;
import org.apache.ode.runtime.exec.platform.task.TaskExecutor.ConvertTarget;
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

public class TaskActionCallable implements Callable<TaskAction.TaskActionState> {

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
	private TaskActionIdImpl id;
	private Document actionInput;

	private org.apache.ode.runtime.exec.cluster.xml.TaskCheck config;
	private LogLevel logLevel = LogLevel.WARNING;
	private String taskId;
	private LinkedList<org.apache.ode.runtime.exec.cluster.xml.Message> msgQueue = new LinkedList<org.apache.ode.runtime.exec.cluster.xml.Message>();
	private final Lock actionUpdateLock = new ReentrantLock();
	private final Condition actionUpdateSignal = actionUpdateLock.newCondition();

	EntityManager pmgr;
	TaskActionImpl taskAction;
	TaskActionActivity exec;
	TaskActionContextImpl taskActionCtx;

	Map<QName, TaskActionDefinition<?, ?>> actions;

	private static final Logger log = Logger.getLogger(TaskActionCallable.class.getName());

	public void config(Node node, org.apache.ode.runtime.exec.cluster.xml.TaskCheck config, LogLevel level, String correlationId, Queue actionRequestor,
			String taskId, TaskActionIdImpl id, Document actionInput) {
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

				TaskActionDefinition def = actions.get(taskAction.name());
				if (def == null) {
					fail(String.format("Unsupported TaskAction %s", taskAction.name().toString()), false);
				}
				taskActionCtx = new TaskActionContextImpl();
				exec = (TaskActionActivity) def.actionExec().get();

				taskAction.setState(TaskAction.TaskActionState.START);
				updateAction();
				exec.start(taskActionCtx,
						convertToObject(actionInput, locateTarget(exec.getClass(), ConvertTarget.OUTPUT), def.jaxbContext()));
				if (taskActionCtx.failed) {
					fail(null, true);
				}
				taskAction.setState(TaskAction.TaskActionState.EXECUTE);
				updateAction();
				if (exec instanceof TaskActionExec) {
					((TaskActionExec) exec).execute();
					updateAction();
				} else if (exec instanceof CoordinatedTaskActionExec) {
					CoordinatedTaskActionExec cexec = (CoordinatedTaskActionExec) exec;
					while (!taskActionCtx.failed && TaskAction.TaskActionState.EXECUTE == taskAction.state()) {
						try {
							actionUpdateLock.lock();
							if (!actionUpdateSignal.await(config.getActionCoordinationTimeout(), TimeUnit.MILLISECONDS)) {
								fail("Coordination timed out", true);
							}
							Object cresult = cexec.execute(convertToObject(taskAction.getCoordinationInput(),
									locateTarget(exec.getClass(), ConvertTarget.COORDINATE_INPUT), def.jaxbContext()));
							taskAction.setCoordinationOutput(convertToDocument(cresult, locateTarget(exec.getClass(), ConvertTarget.COORDINATE_OUTPUT),
									def.jaxbContext()));
							updateAction();
						} catch (InterruptedException e) {
							fail(String.format("Coordination interupted", e.getMessage()), true);
						} finally {
							actionUpdateLock.unlock();
						}
					}
				}
				if (taskActionCtx.failed) {
					fail(null, true);
				}
				taskAction.setState(TaskAction.TaskActionState.FINISH);
				updateAction();
				Object result = exec.finish();
				if (taskActionCtx.failed) {
					fail(null, true);
				}
				taskAction.setOutput(convertToDocument(result, locateTarget(exec.getClass(), ConvertTarget.OUTPUT), def.jaxbContext()));
				if (exec instanceof TaskActionTransaction) {
					taskAction.setState(TaskAction.TaskActionState.PENDING);
					updateAction();
					try {
						actionUpdateLock.lock();
						if (!actionUpdateSignal.await(config.getActionTransactionTimeout(), TimeUnit.MILLISECONDS)) {
							fail(String.format("Transaction timed out"), true);
						}
						((TaskActionTransaction) exec).complete();
						if (taskAction.state() == TaskAction.TaskActionState.COMMIT) {
							taskAction.setState(TaskAction.TaskActionState.COMPLETE);
						}
						updateAction();
					} catch (InterruptedException e) {
						fail(String.format("Transaction interupted", e.getMessage()), true);
					} finally {
						actionUpdateLock.unlock();
					}
				} else {
					taskAction.setState(TaskAction.TaskActionState.COMPLETE);
					updateAction();
				}

			} catch (JMSException je) {
				log.log(Level.SEVERE, "", je);
				fail(je.getMessage(), false);
			} catch (PlatformException pe) {
				throw pe;

			} finally {
				try {
					actionUpdateQueueConnection.close();
					msgUpdateTopicConnection.close();
				} catch (JMSException je) {
					log.log(Level.SEVERE, "", je);
				}
			}

		} finally {
			pmgr.close();

		}
		return taskAction.state();

	}

	public void externalUpdate(org.apache.ode.runtime.exec.cluster.xml.TaskAction xmlAction) {
		try {
			actionUpdateLock.lock();
			taskAction.setState(TaskAction.TaskActionState.valueOf(xmlAction.getState().value()));
			taskAction.setCoordinationInput(xmlAction.getCoordinationInput());
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
		try {
			org.apache.ode.runtime.exec.cluster.xml.Message xmlMessage = convert(m);
			if (logLevel.ordinal() >= xmlMessage.getLevel().ordinal()) {
				msgQueue.add(xmlMessage);

				BytesMessage jmsMessage = msgUpdateSession.createBytesMessage();
				Marshaller marshaller = CLUSTER_JAXB_CTX.createMarshaller();
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				marshaller.marshal(new JAXBElement(new QName(CLUSTER_NAMESPACE, "Message"), org.apache.ode.runtime.exec.cluster.xml.Message.class, xmlMessage),
						bos);
				jmsMessage.writeBytes(bos.toByteArray());
				jmsMessage.setJMSCorrelationID(correlationId);
				msgUpdatePublisher.publish(jmsMessage);
			}
		} catch (Exception je) {
			log.log(Level.SEVERE, "", je);
		}
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
		updateAction();

		throw new PlatformException(msg);

	}

	public org.apache.ode.runtime.exec.cluster.xml.Message convert(MessageImpl message) {
		org.apache.ode.runtime.exec.cluster.xml.Message xmlMessage = new org.apache.ode.runtime.exec.cluster.xml.Message();
		xmlMessage.setLevel(org.apache.ode.runtime.exec.cluster.xml.LogLevel.fromValue(message.level().toString()));
		xmlMessage.setCode(BigInteger.valueOf(message.code()));
		xmlMessage.setTimestamp(Calendar.getInstance());
		xmlMessage.setValue(message.message());
		return xmlMessage;
	}

	public org.apache.ode.runtime.exec.cluster.xml.TaskAction convert(TaskActionImpl action) {
		org.apache.ode.runtime.exec.cluster.xml.TaskAction xmlAction = new org.apache.ode.runtime.exec.cluster.xml.TaskAction();
		xmlAction.setActionId(String.valueOf(((TaskActionIdImpl) action.id()).taskActionId));
		xmlAction.setTaskId(taskId);
		Calendar mod = Calendar.getInstance();
		mod.setTime(action.modified());
		xmlAction.setModified(mod);
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
				xmlAction.setCoordinationOutput(out);
			}
			break;
		case FINISH:
			xmlAction.setOutput(action.getOutput());
			break;
		case ROLLBACK:
		case COMMIT:
		case COMPLETE:
		case FAILED:
			Calendar fin = Calendar.getInstance();
			fin.setTime(action.finish());
			xmlAction.setFinished(fin);
			break;

		}
		return xmlAction;
	}

	public static Class<?> locateTarget(Class<?> clazz, ConvertTarget target) throws PlatformException {

		Class<?> targetClass = null;
		for (Type t : clazz.getClass().getGenericInterfaces()) {
			if (t instanceof TaskActionActivity) {
				if (target == ConvertTarget.INPUT) {
					targetClass = (Class<?>) ((ParameterizedType) t).getActualTypeArguments()[0];
					break;
				} else if (target == ConvertTarget.OUTPUT) {
					targetClass = (Class<?>) ((ParameterizedType) t).getActualTypeArguments()[1];
					break;
				}
			} else if (t instanceof CoordinatedTaskActionExec) {
				if (target == ConvertTarget.COORDINATE_INPUT) {
					targetClass = (Class<?>) ((ParameterizedType) t).getActualTypeArguments()[0];
					break;
				} else if (target == ConvertTarget.COORDINATE_OUTPUT) {
					targetClass = (Class<?>) ((ParameterizedType) t).getActualTypeArguments()[1];
					break;
				}
			}
		}
		if (targetClass == null) {
			throw new PlatformException(String.format("Unable to convert target %s on class %s", target.name(), clazz.getName()));
		}
		return targetClass;
	}
}