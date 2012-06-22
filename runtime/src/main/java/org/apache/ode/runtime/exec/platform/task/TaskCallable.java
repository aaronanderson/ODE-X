package org.apache.ode.runtime.exec.platform.task;

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
import javax.xml.namespace.QName;

import org.apache.ode.runtime.exec.platform.MessageImpl;
import org.apache.ode.runtime.exec.platform.NodeImpl.NodeId;
import org.apache.ode.runtime.exec.platform.NodeImpl.TaskCheck;
import org.apache.ode.runtime.exec.platform.TargetImpl;
import org.apache.ode.runtime.exec.platform.task.TaskImpl.TaskIdImpl;
import org.apache.ode.spi.exec.Message.LogLevel;
import org.apache.ode.spi.exec.Node;
import org.apache.ode.spi.exec.PlatformException;
import org.apache.ode.spi.exec.Target;
import org.apache.ode.spi.exec.Task.TaskActionCoordinator;
import org.apache.ode.spi.exec.Task.TaskActionDefinition;
import org.apache.ode.spi.exec.Task.TaskActionRequest;
import org.apache.ode.spi.exec.Task.TaskActionResponse;
import org.apache.ode.spi.exec.Task.TaskDefinition;
import org.apache.ode.spi.exec.Task.TaskState;
import org.w3c.dom.Document;

public class TaskCallable implements Callable<TaskState> {
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

	Map<QName, TaskDefinition> tasks;
	Map<QName, TaskActionDefinition> actions;

	private TaskIdImpl id;
	private Document taskInput;
	private Target[] targets;
	EntityManager pmgr;
	TaskImpl task;

	private static final Logger log = Logger.getLogger(TaskCallable.class.getName());

	public TaskCallable(Node node, Queue taskRequestor, String taskCorrelationId, TaskIdImpl id, Document taskInput, Target... targets) {
		this.taskRequestor = taskRequestor;
		this.taskCorrelationId = taskCorrelationId;
		this.id = id;
		this.taskInput = taskInput;
		this.targets = targets;
		this.tasks = node.getTaskDefinitions();
		this.actions = node.getTaskActionDefinitions();
	}

	public class TaskActionExecution {
		TaskActionCoordinator owner;
		TaskActionImpl action;
		TaskActionResponse response;
		TaskActionRequest request;
		Set<TaskActionExecution> prevDependencies = new HashSet<TaskActionExecution>();
		Set<TaskActionExecution> nextDependencies = new HashSet<TaskActionExecution>();
		private Queue actionRequestor;
		private QueueReceiver actionRequestorReceiver;
		private String actionCorrelationId;

	}

	public class TaskCoordinatorState {
		TaskActionCoordinator coordinator;
		Set<TaskActionExecution> requests = new HashSet<TaskActionExecution>();
	}

	@Override
	public TaskState call() throws PlatformException {
		pmgr = pmgrFactory.createEntityManager();
		try {
			task = pmgr.find(TaskImpl.class, id.id());
			try {
				taskUpdateQueueConnection = queueConFactory.createQueueConnection();
				taskUpdateSession = taskUpdateQueueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
				taskActionSender = taskUpdateSession.createSender(taskQueue);
				taskRequestorSender = taskUpdateSession.createSender(taskRequestor);
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
		return task.state();
	}

	public void init(Set<TaskCoordinatorState> coordinators, Map<QName, TaskActionExecution> actionExecutions) throws PlatformException {

		TaskDefinition def = tasks.get(task.name());

		for (TaskActionCoordinator coordinator : def.coordinators()) {
			TaskCoordinatorState taskCordState = new TaskCoordinatorState();
			taskCordState.coordinator = coordinator;
			Set<TaskActionRequest> requests = coordinator.init(taskInput, targets);
			for (TaskActionRequest canidate : requests) {
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
			task.setState(TaskState.FAIL);
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

}