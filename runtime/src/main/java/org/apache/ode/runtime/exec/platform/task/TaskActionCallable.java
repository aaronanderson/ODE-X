package org.apache.ode.runtime.exec.platform.task;

import static org.apache.ode.runtime.exec.platform.NodeImpl.CLUSTER_JAXB_CTX;
import static org.apache.ode.spi.exec.Node.CLUSTER_NAMESPACE;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.Callable;
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
import org.apache.ode.spi.exec.Message.LogLevel;
import org.apache.ode.spi.exec.Node;
import org.apache.ode.spi.exec.PlatformException;
import org.apache.ode.spi.exec.Task.TaskActionContext;
import org.apache.ode.spi.exec.Task.TaskActionDefinition;
import org.apache.ode.spi.exec.Task.TaskActionExec;
import org.apache.ode.spi.exec.Task.TaskActionId;
import org.apache.ode.spi.exec.Task.TaskActionState;
import org.w3c.dom.Document;

public class TaskActionCallable implements Callable<TaskActionState> {

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

	private LogLevel logLevel = LogLevel.WARNING;
	private String taskId;
	private LinkedList<org.apache.ode.runtime.exec.cluster.xml.Message> msgQueue = new LinkedList<org.apache.ode.runtime.exec.cluster.xml.Message>();
	private final Lock actionUpdateLock = new ReentrantLock();
	private final Condition actionUpdateSignal = actionUpdateLock.newCondition();
	private boolean interactive;

	EntityManager pmgr;
	TaskActionImpl taskAction;
	TaskActionExec exec;
	TaskActionContextImpl taskActionCtx;

	Map<QName, TaskActionDefinition> actions;

	private static final Logger log = Logger.getLogger(TaskActionCallable.class.getName());

	public TaskActionCallable(Node node, LogLevel level, String correlationId, Queue actionRequestor, String taskId, TaskActionIdImpl id, Document actionInput) {
		this.actions = node.getTaskActionDefinitions();
		this.logLevel = level;
		this.correlationId = correlationId;
		this.actionRequestor = actionRequestor;
		this.taskId = taskId;
		this.id = id;
		this.actionInput = actionInput;
	}

	public void cancel() {
		try {
			actionUpdateLock.lock();
			actionUpdateSignal.signal();
		} finally {
			actionUpdateLock.unlock();
		}
	}

	public void executeUpdate() {
		try {
			actionUpdateLock.lock();
			actionUpdateSignal.signal();
		} finally {
			actionUpdateLock.unlock();
		}
	}

	@Override
	public TaskActionState call() throws PlatformException {
		pmgr = pmgrFactory.createEntityManager();
		try {
			taskAction = pmgr.find(TaskActionImpl.class, id.id());
			try {
				actionUpdateQueueConnection = taskQueueConFactory.createQueueConnection();
				actionUpdateSession = actionUpdateQueueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
				if (actionRequestor != null) {
					actionRequestorSender = actionUpdateSession.createSender(actionRequestor);
				}

				msgUpdateTopicConnection = msgTopicConFactory.createTopicConnection();
				msgUpdateSession = msgUpdateTopicConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
				msgUpdatePublisher = msgUpdateSession.createPublisher(msgUpdateTopic);
			} catch (JMSException je) {
				log.log(Level.SEVERE, "", je);
				rollback(je.getMessage());
			}
			TaskActionDefinition def = actions.get(taskAction.name());
			if (def == null) {
				rollback(String.format("Unsupported TaskAction %s", taskAction.name().toString()));
			}
			taskActionCtx = new TaskActionContextImpl();
			exec = def.actionExec().get();
			interactive = def.interactive();
			if (interactive) {
				try {
					taskAction.setState(TaskActionState.START);
					updateAction();
					exec.start(taskActionCtx, actionInput);
					taskAction.setState(TaskActionState.EXECUTE);
					updateAction();
					while (taskAction.state() == TaskActionState.EXECUTE) {
						try {
							actionUpdateLock.lock();
							actionUpdateSignal.await();
							Document coordination = exec.execute(taskActionCtx, taskAction.getCoordination());
							taskAction.setCoordination(coordination);
							updateAction();
						} catch (InterruptedException e) {
							// ignore
						} finally {
							actionUpdateLock.unlock();
						}
					}

					taskAction.setState(TaskActionState.FINISH);
					updateAction();
					try {
						actionUpdateLock.lock();
						actionUpdateSignal.await();
						Document result = exec.finish(taskActionCtx);
						taskAction.setResult(result);
						updateAction();
					} catch (InterruptedException e) {
						// ignore
					} finally {
						actionUpdateLock.unlock();
					}

				} catch (PlatformException pe) {
					throw pe;
				}
			} else {
				try {
					taskAction.setState(TaskActionState.START);
					updateAction();
					exec.start(taskActionCtx, actionInput);
					taskAction.setState(TaskActionState.EXECUTE);
					updateAction();
					exec.execute(taskActionCtx, null);
					taskAction.setState(TaskActionState.FINISH);
					updateAction();
					Document result = exec.finish(taskActionCtx);
					taskAction.setState(TaskActionState.COMPLETE);
					taskAction.setResult(result);
					updateAction();
				} catch (PlatformException pe) {
					throw pe;
				}
			}

		} finally {
			pmgr.close();
			try {
				actionUpdateQueueConnection.close();
				msgUpdateTopicConnection.close();
			} catch (JMSException je) {
				log.log(Level.SEVERE, "", je);
			}
		}
		return taskAction.state();

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
			}
			BytesMessage jmsMessage = msgUpdateSession.createBytesMessage();
			Marshaller marshaller = CLUSTER_JAXB_CTX.createMarshaller();
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			marshaller
					.marshal(new JAXBElement(new QName(CLUSTER_NAMESPACE, "Message"), org.apache.ode.runtime.exec.cluster.xml.Message.class, xmlMessage), bos);
			jmsMessage.writeBytes(bos.toByteArray());
			jmsMessage.setJMSCorrelationID(correlationId);
			msgUpdatePublisher.publish(jmsMessage);
		} catch (Exception je) {
			log.log(Level.SEVERE, "", je);
		}
	}

	public class TaskActionContextImpl implements TaskActionContext {

		public TaskActionId id() {
			return taskAction.id();
		}

		public QName name() {
			return taskAction.name();
		}

		public void log(LogLevel level, int code, String message) {
			updateLog(level, code, message);
		}

		public TaskActionState getState() {
			return taskAction.state();
		}

		public void updateState(TaskActionState state) throws PlatformException {
			taskAction.setState(state);
			updateAction();
		}

		@Override
		public boolean interactive() {
			return interactive;
		}

	}

	public void rollback(String msg) throws PlatformException {
		taskAction.setState(TaskActionState.ROLLBACK);
		if (exec != null) {
			exec.finish(taskActionCtx);
		}
		updateLog(LogLevel.ERROR, 0, msg);
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
		case EXECUTE:
		case FINISH:
		case ROLLBACK:
		case COMMIT:
		case COMPLETE:
			Calendar start = Calendar.getInstance();
			start.setTime(action.start());
			xmlAction.setStart(start);
			Calendar fin = Calendar.getInstance();
			fin.setTime(action.finish());
			xmlAction.setFinished(fin);

		}
		return xmlAction;
	}
}