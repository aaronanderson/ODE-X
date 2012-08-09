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
package org.apache.ode.runtime.exec.platform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.ode.runtime.exec.modules.ServerModule.VMServerModule;
import org.apache.ode.runtime.exec.task.test.xml.MultiTaskInput;
import org.apache.ode.runtime.exec.task.test.xml.MultiTaskOutput;
import org.apache.ode.runtime.exec.task.test.xml.SingleTaskInput;
import org.apache.ode.runtime.exec.task.test.xml.SingleTaskOutput;
import org.apache.ode.spi.exec.Component;
import org.apache.ode.spi.exec.Message.LogLevel;
import org.apache.ode.spi.exec.Node;
import org.apache.ode.spi.exec.Platform;
import org.apache.ode.spi.exec.PlatformException;
import org.apache.ode.spi.exec.target.Target;
import org.apache.ode.spi.exec.target.TargetAll;
import org.apache.ode.spi.exec.target.TargetNode;
import org.apache.ode.spi.exec.task.Task;
import org.apache.ode.spi.exec.task.Task.TaskId;
import org.apache.ode.spi.exec.task.Task.TaskState;
import org.apache.ode.spi.exec.task.TaskAction;
import org.apache.ode.spi.exec.task.TaskActionContext;
import org.apache.ode.spi.exec.task.TaskActionCoordinator;
import org.apache.ode.spi.exec.task.TaskActionDefinition;
import org.apache.ode.spi.exec.task.TaskActionExec;
import org.apache.ode.spi.exec.task.TaskCallback;
import org.apache.ode.spi.exec.task.TaskContext;
import org.apache.ode.spi.exec.task.TaskDefinition;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.inject.AbstractModule;
import com.mycila.inject.jsr250.Jsr250;
import com.mycila.inject.jsr250.Jsr250Injector;

public class TaskTest {
	private static final Logger log = Logger.getLogger(TaskTest.class.getName());
	private static Jsr250Injector injector1;
	private static Jsr250Injector injector2;
	private static JAXBContext taskJAXBContext;

	private static Node node1;
	private static Node node2;
	private static Platform platform1;
	private static Platform platform2;

	private static final org.apache.ode.runtime.exec.task.test.xml.ObjectFactory taskObjectFactory = new org.apache.ode.runtime.exec.task.test.xml.ObjectFactory();

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		taskJAXBContext = JAXBContext.newInstance("org.apache.ode.spi.exec.platform.xml:org.apache.ode.runtime.exec.task.test.xml");

		injector1 = Jsr250.createInjector(new VMServerModule("vm://task?broker.persistent=true&broker.useJmx=false&broker.dataDirectory=target/activemq-data",
				"tcluster", "node1"), new TaskTestModule());
		HealthCheckTest.loadTestClusterConfig(injector1, "tcluster");
		injector2 = Jsr250.createInjector(new VMServerModule("vm://task?broker.persistent=true&broker.useJmx=false&broker.dataDirectory=target/activemq-data",
				"tcluster", "node2"), new TaskTestModule());

		node1 = injector1.getInstance(Node.class);
		assertNotNull(node1);
		node1.online();
		TaskTestComponent component1 = injector1.getInstance(TaskTestComponent.class);
		assertNotNull(component1);
		platform1 = injector1.getInstance(Platform.class);
		assertNotNull(platform1);

		node2 = injector2.getInstance(Node.class);
		assertNotNull(node2);
		node2.online();
		TaskTestComponent component2 = injector2.getInstance(TaskTestComponent.class);
		assertNotNull(component2);
		platform2 = injector1.getInstance(Platform.class);
		assertNotNull(platform1);

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		node1.offline();
		node2.offline();
		injector1.destroy();
		injector2.destroy();
	}

	public static class TaskTestModule extends AbstractModule {

		@Override
		protected void configure() {
			bind(TaskTestComponent.class);
			bind(SingleTaskActionExec.class);
			bind(MultiTaskActionExec.class);
		}

	}

	public void testSingleTaskAction(QName taskName, String nodeId) throws Exception {
		assertTrue(node1.getComponents().contains(TaskTestComponent.COMPONENT_NAME));
		//Execute operations on platform

		Marshaller m = taskJAXBContext.createMarshaller();
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		Document doc = dbf.newDocumentBuilder().newDocument();
		SingleTaskInput in = new SingleTaskInput();
		in.setValue("SingleTaskInput");
		JAXBElement<SingleTaskInput> element = taskObjectFactory.createSingleTaskInput(in);
		m.marshal(element, doc);

		platform1.setLogLevel(LogLevel.DEBUG);
		TargetNode target = platform1.createTarget(nodeId, TargetNode.class);
		Future<Document> result = platform1.execute(taskName, doc, null, target);
		assertNotNull(result);
		Document res = result.get();//result.get(5, TimeUnit.SECONDS);
		assertNotNull(res);
		Unmarshaller u = taskJAXBContext.createUnmarshaller();
		JAXBElement<SingleTaskOutput> xout = (JAXBElement<SingleTaskOutput>) u.unmarshal(res);
		SingleTaskOutput out = xout.getValue();
		assertNotNull(out);
		assertEquals("SingleTaskOutput", out.getValue());
	}

	@Test
	public void localTaskActionTest() throws Exception {
		testSingleTaskAction(TaskTestComponent.SINGLE_TASK_NAME, "node1");
	}

	@Test
	public void remoteTaskActionTest() throws Exception {
		testSingleTaskAction(TaskTestComponent.SINGLE_TASK_NAME, "node2");
	}

	@Test
	public void localTaskActionAsyncTest() throws Exception {

		Marshaller m = taskJAXBContext.createMarshaller();
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		Document doc = dbf.newDocumentBuilder().newDocument();
		SingleTaskInput in = new SingleTaskInput();
		in.setValue("SingleTaskInput");
		JAXBElement<SingleTaskInput> element = taskObjectFactory.createSingleTaskInput(in);
		m.marshal(element, doc);

		platform1.setLogLevel(LogLevel.DEBUG);
		TargetNode target = platform1.createTarget("node1", TargetNode.class);
		TaskId id = platform1.executeAsync(TaskTestComponent.SINGLE_TASK_NAME, doc, null, target);
		assertNotNull(id);
		Task status = platform1.taskStatus(id);
		assertNotNull(status);
		long start = System.currentTimeMillis();
		while (true) {
			//System.out.format("task state %s\n", status.state());
			status.refresh();
			if (TaskState.COMPLETE == status.state()) {
				break;
			} else if (System.currentTimeMillis() - start > 10000l) {
				fail(String.format("Task timed out: %s", status.state()));
			}
		}

		Document res = status.output();
		assertNotNull(res);
		Unmarshaller u = taskJAXBContext.createUnmarshaller();
		JAXBElement<SingleTaskOutput> xout = (JAXBElement<SingleTaskOutput>) u.unmarshal(res);
		SingleTaskOutput out = xout.getValue();
		assertNotNull(out);
		assertEquals("SingleTaskOutput", out.getValue());
		Set<TaskAction> actions = status.actions();
		assertNotNull(actions);
		assertEquals(1, actions.size());
		//TODO examine more of the entity to make sure all targets and messages are present

	}

	@Test
	public void dependencyTaskActionTest() throws Exception {
		testSingleTaskAction(TaskTestComponent.SINGLE_TASK_DEP_NAME, "node1");
	}

	@Test
	public void multipleTaskActionTest() throws Exception {
		Marshaller m = taskJAXBContext.createMarshaller();
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		Document doc = dbf.newDocumentBuilder().newDocument();
		SingleTaskInput in = new SingleTaskInput();
		in.setValue("MultiTaskInput");
		JAXBElement<SingleTaskInput> element = taskObjectFactory.createSingleTaskInput(in);
		m.marshal(element, doc);

		platform1.setLogLevel(LogLevel.DEBUG);
		TargetAll target = platform1.createTarget(null, TargetAll.class);
		Future<Document> result = platform1.execute(TaskTestComponent.MULTI_TASK_NAME, doc, null, target);
		assertNotNull(result);
		Document res = result.get();//result.get(5, TimeUnit.SECONDS);
		assertNotNull(res);
		Unmarshaller u = taskJAXBContext.createUnmarshaller();
		JAXBElement<MultiTaskOutput> xout = (JAXBElement<MultiTaskOutput>) u.unmarshal(res);
		MultiTaskOutput out = xout.getValue();
		assertNotNull(out);
		assertEquals("MultiTaskOutput", out.getValue());
	}

	//@Test
	public void commitTaskActionTest() throws Exception {

	}

	//@Test
	public void rollbackTaskActionTest() throws Exception {

	}

	//@Test
	public void failTaskTest() throws Exception {

	}

	//@Test
	public void failTaskActionTest() throws Exception {

	}

	//@Test
	public void cancelTaskTest() throws Exception {

	}

	//@Test
	public void timeoutTaskTest() throws Exception {

	}

	/*
	 @Test
	public void taskListenerTest() throws Exception {
	final Set<Integer> codes = new HashSet<Integer>();

	TaskListener listener = new TaskListener() {

		@Override
		public LogLevel levelFilter() {
			return LogLevel.DEBUG;
		}

		@Override
		public void message(MessageEvent message) {
			log.info(message.toString());
			codes.add(message.message().code());
		}

	};
	
	platform1.registerListener(listener);
	try {
	
	} finally {
		platform1.unregisterListener(listener);
	}
	}
	
	 
	@Test
	public void mqTaskActionTest() throws Exception {
	QueueConnectionFactory taskFactory = injector1.getInstance(Key.get(QueueConnectionFactory.class, TaskCheck.class));
	Queue taskQueue = injector1.getInstance(Key.get(Queue.class, TaskCheck.class));
	QueueConnection taskQueueConnection = taskFactory.createQueueConnection();
	try {
		QueueSession session = taskQueueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
		QueueSender sender = session.createSender(taskQueue);
		Queue responseQueue = session.createTemporaryQueue();
		QueueReceiver receiver = session.createReceiver(responseQueue);

		Marshaller marshaller = CLUSTER_JAXB_CTX.createMarshaller();
		org.apache.ode.spi.exec.platform.xml.TaskAction taskAction = new org.apache.ode.spi.exec.platform.xml.TaskAction();
		taskAction.setActionId(Node.NODE_MQ_PROP_VALUE_NEW);
		taskAction.setState(TaskActionState.SUBMIT);
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		marshaller.marshal(taskAction, payload);
		BytesMessage message = session.createBytesMessage();
		message.setStringProperty(Node.NODE_MQ_PROP_NODE, "node1");
		message.setStringProperty(Node.NODE_MQ_PROP_ACTIONID, Node.NODE_MQ_PROP_VALUE_NEW);
		message.writeBytes(payload.toByteArray());
		message.setJMSCorrelationID(message.getJMSMessageID());
		message.setJMSReplyTo(responseQueue);
		sender.send(message);
		BytesMessage responseMessage = (BytesMessage) receiver.receive(5000l);
		assertNotNull(responseMessage);

	} finally {
		taskQueueConnection.close();
	}
	}
	*/

	public static class TaskTestComponent implements Component {

		public static final String TEST_NS = "http://ode.apache.org/TaskTest";
		public static final QName COMPONENT_NAME = new QName(TEST_NS, "TaskComponent");

		public static final QName SINGLE_TASK_NAME = new QName(TEST_NS, "SingleTask");
		public static final QName SINGLE_TASK_COORD_NAME = new QName(TEST_NS, "SingleTaskCoordinator");
		public static final QName SINGLE_TASK_DEP_NAME = new QName(TEST_NS, "SingleDependencyTask");
		public static final QName SINGLE_TASK_DEP_COORD_NAME = new QName(TEST_NS, "SingleDependencyTaskCoordinator");
		public static final QName MULTI_TASK_NAME = new QName(TEST_NS, "MultiTask");
		public static final QName MULTI_TASK_COORD_NAME = new QName(TEST_NS, "MultiTaskTaskCoordinator");

		public static final QName SINGLE_ACTION_NAME = new QName(TEST_NS, "SingleTaskAction");
		public static final QName SINGLE_ACTION_DEP_NAME = new QName(TEST_NS, "SingleTaskActionDependency");
		public static final QName MULTI_ACTION1_NAME = new QName(TEST_NS, "MultiTaskTask1");
		public static final QName MULTI_ACTION2_NAME = new QName(TEST_NS, "MultiTaskTask2");

		@Inject
		Node node;

		@Inject
		Provider<SingleTaskActionExec> singleAction;

		@Inject
		Provider<MultiTaskActionExec> multiAction;

		@PostConstruct
		public void init() {
			log.fine("Initializing TaskTestComponent");
			node.registerComponent(this);
			log.fine("TaskTestComponent Initialized");

		}

		@Override
		public QName name() {
			return COMPONENT_NAME;
		}

		@Override
		public List<InstructionSet> instructionSets() {
			return Collections.EMPTY_LIST;
		}

		@Override
		public List<TaskDefinition> tasks() {
			ArrayList<TaskDefinition> defs = new ArrayList<TaskDefinition>();
			defs.add(new TaskDefinition(SINGLE_TASK_NAME, new SingleTaskActionCoordinator(), taskJAXBContext));
			defs.add(new TaskDefinition(SINGLE_TASK_DEP_NAME, new SingleDependencyTaskActionCoordinator(), taskJAXBContext));
			defs.add(new TaskDefinition(MULTI_TASK_NAME, new MultiTaskActionCoordinator(), taskJAXBContext));
			return defs;
		}

		@Override
		public List<TaskActionDefinition> actions() {
			ArrayList<TaskActionDefinition> defs = new ArrayList<TaskActionDefinition>();
			defs.add(new TaskActionDefinition(SINGLE_ACTION_NAME, (Set<QName>) Collections.EMPTY_SET, singleAction, taskJAXBContext));
			Set<QName> dependencies = new HashSet<QName>();
			dependencies.add(SINGLE_ACTION_NAME);
			defs.add(new TaskActionDefinition(SINGLE_ACTION_DEP_NAME, dependencies, singleAction, taskJAXBContext));
			defs.add(new TaskActionDefinition(MULTI_ACTION1_NAME, (Set<QName>) Collections.EMPTY_SET, multiAction, taskJAXBContext));
			Set<QName> deps = new HashSet<QName>();
			deps.add(MULTI_ACTION1_NAME);
			defs.add(new TaskActionDefinition(MULTI_ACTION2_NAME, deps, multiAction, taskJAXBContext));
			return defs;
		}

		@Override
		public void online() throws PlatformException {
			// TODO Auto-generated method stub

		}

		@Override
		public void offline() throws PlatformException {
			// TODO Auto-generated method stub

		}

	}

	public static Document testDoc(String elementName, String input) {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.newDocument();
			Element root = doc.createElementNS(TaskTestComponent.TEST_NS, elementName);
			root.setTextContent(input);
			doc.appendChild(root);
			return doc;
		} catch (Exception e) {
			log.log(Level.SEVERE, "", e);
			return null;
		}

	}

	public static class SingleTaskActionCoordinator implements TaskActionCoordinator<SingleTaskInput, SingleTaskInput, SingleTaskOutput, SingleTaskOutput> {

		@Override
		public Set<TaskActionRequest<SingleTaskInput>> init(TaskContext ctx, SingleTaskInput input, String localNodeId, TaskCallback<?, ?> callback,
				Target... targets) {
			assertEquals("SingleTaskInput", input.getValue());
			input.setValue("SingleActionInput");
			String[] nodeIds = TaskDefinition.targetsToNodeIds(targets);
			HashSet<TaskActionRequest<SingleTaskInput>> actions = new HashSet<TaskActionRequest<SingleTaskInput>>();
			TaskActionRequest<SingleTaskInput> req = new TaskActionRequest<SingleTaskInput>(TaskTestComponent.SINGLE_ACTION_NAME, nodeIds[0], input);
			actions.add(req);
			return actions;
		}

		/*
				@Override
				public void refresh(TaskActionResponse<SingleTaskOutput> action) {
					assertEquals(TaskTestComponent.SINGLE_ACTION_NAME, action.action);
				}
		*/
		@Override
		public boolean update(TaskActionRequest<SingleTaskInput> request, Set<TaskActionResponse<SingleTaskOutput>> dependencyResponses) {
			fail();//should not be called
			return false;
		}

		@Override
		public SingleTaskOutput finish(Set<TaskActionResponse<SingleTaskOutput>> actions, SingleTaskOutput output) {
			assertEquals(1, actions.size());
			TaskActionResponse<SingleTaskOutput> response = actions.iterator().next();
			assertEquals(TaskTestComponent.SINGLE_ACTION_NAME, response.action);
			assertTrue(response.success);
			assertNotNull(response.output);
			assertEquals("SingleActionOutput", response.output.getValue());
			output = new SingleTaskOutput();
			output.setValue("SingleTaskOutput");
			return output;
		}

		@Override
		public QName name() {
			return TaskTestComponent.SINGLE_TASK_COORD_NAME;
		}

		@Override
		public Set<QName> dependencies() {
			return Collections.EMPTY_SET;
		}

	}

	public static class SingleDependencyTaskActionCoordinator implements
			TaskActionCoordinator<SingleTaskInput, SingleTaskInput, SingleTaskOutput, SingleTaskOutput> {

		@Override
		public Set<TaskActionRequest<SingleTaskInput>> init(TaskContext ctx, SingleTaskInput input, String localNodeId, TaskCallback<?, ?> callback,
				Target... targets) {
			assertEquals("SingleTaskInput", input.getValue());
			input.setValue("SingleActionInput");
			String[] nodeIds = TaskDefinition.targetsToNodeIds(targets);
			HashSet<TaskActionRequest<SingleTaskInput>> actions = new HashSet<TaskActionRequest<SingleTaskInput>>();
			TaskActionRequest<SingleTaskInput> req = new TaskActionRequest<SingleTaskInput>(TaskTestComponent.SINGLE_ACTION_NAME, nodeIds[0], input);
			actions.add(req);
			req = new TaskActionRequest<SingleTaskInput>(TaskTestComponent.SINGLE_ACTION_DEP_NAME, nodeIds[0], input);
			actions.add(req);
			return actions;
		}

		/*
		@Override
		public void refresh(TaskActionResponse<SingleTaskOutput> action) {

		}
		*/
		@Override
		public boolean update(TaskActionRequest<SingleTaskInput> request, Set<TaskActionResponse<SingleTaskOutput>> dependencyResponses) {
			assertEquals(TaskTestComponent.SINGLE_ACTION_DEP_NAME, request.action);
			assertEquals(1, dependencyResponses.size());
			TaskActionResponse<SingleTaskOutput> response = dependencyResponses.iterator().next();
			assertTrue(response.success);
			assertNotNull(response.output);
			assertEquals("SingleActionOutput", response.output.getValue());
			request.input.setValue("SingleActionDepInput");
			return true;
		}

		@Override
		public SingleTaskOutput finish(Set<TaskActionResponse<SingleTaskOutput>> actions, SingleTaskOutput output) {
			assertEquals(2, actions.size());
			for (TaskActionResponse<SingleTaskOutput> response : actions) {
				assertTrue(response.success);
				assertNotNull(response.output);
				if (TaskTestComponent.SINGLE_ACTION_NAME.equals(response.action)) {
					assertEquals("SingleActionOutput", response.output.getValue());
				} else if (TaskTestComponent.SINGLE_ACTION_DEP_NAME.equals(response.action)) {
					assertEquals("SingleActionDepOutput", response.output.getValue());
				}
			}
			output = new SingleTaskOutput();
			output.setValue("SingleTaskOutput");
			return output;
		}

		@Override
		public QName name() {
			return TaskTestComponent.SINGLE_TASK_DEP_COORD_NAME;
		}

		@Override
		public Set<QName> dependencies() {
			return Collections.EMPTY_SET;
		}

	}

	public static class SingleTaskActionExec implements TaskActionExec<SingleTaskInput, SingleTaskOutput> {
		TaskActionContext ctx;
		SingleTaskOutput out = new SingleTaskOutput();

		@Override
		public void start(TaskActionContext ctx, SingleTaskInput input) {
			this.ctx = ctx;
			ctx.log(LogLevel.INFO, 1, "start");
			if (TaskTestComponent.SINGLE_ACTION_NAME.equals(ctx.name())) {
				assertEquals("SingleActionInput", input.getValue());
			} else if (TaskTestComponent.SINGLE_ACTION_DEP_NAME.equals(ctx.name())) {
				assertEquals("SingleActionDepInput", input.getValue());

			}
		}

		@Override
		public void execute() {
			ctx.log(LogLevel.INFO, 2, "execute");
			if (TaskTestComponent.SINGLE_ACTION_NAME.equals(ctx.name())) {
				out.setValue("SingleActionOutput");
			} else if (TaskTestComponent.SINGLE_ACTION_DEP_NAME.equals(ctx.name())) {
				out.setValue("SingleActionDepOutput");

			}
		}

		@Override
		public SingleTaskOutput finish() {
			ctx.log(LogLevel.INFO, 3, "finish");
			return out;
		}

	}

	public static class MultiTaskActionCoordinator implements TaskActionCoordinator<MultiTaskInput, MultiTaskInput, MultiTaskOutput, MultiTaskOutput> {

		@Override
		public Set<TaskActionRequest<MultiTaskInput>> init(TaskContext ctx, MultiTaskInput input, String localNodeId, TaskCallback<?, ?> callback,
				Target... targets) {
			String[] nodeIds = TaskDefinition.targetsToNodeIds(targets);
			assertEquals(2, nodeIds.length);
			HashSet<TaskActionRequest<MultiTaskInput>> actions = new HashSet<TaskActionRequest<MultiTaskInput>>();
			for (String nodeId : nodeIds) {
				MultiTaskInput actionInput = new MultiTaskInput();
				actionInput.setValue("MultiAction1Input");
				actions.add(new TaskActionRequest<MultiTaskInput>(TaskTestComponent.MULTI_ACTION1_NAME, nodeId, actionInput));
				if (localNodeId.equals(nodeId)) {
					actionInput = new MultiTaskInput();
					actionInput.setValue(null);
					actions.add(new TaskActionRequest<MultiTaskInput>(TaskTestComponent.MULTI_ACTION2_NAME, localNodeId, actionInput));
				}
			}
			return actions;
		}

		/*
		@Override
		public void refresh(TaskActionResponse<MultiTaskOutput> action) {
			// TODO Auto-generated method stub

		}
		*/

		@Override
		public boolean update(TaskActionRequest<MultiTaskInput> request, Set<TaskActionResponse<MultiTaskOutput>> dependencyResponses) {
			assertEquals(TaskTestComponent.MULTI_ACTION2_NAME, request.action);
			assertEquals(2, dependencyResponses.size());
			for (TaskActionResponse<MultiTaskOutput> response : dependencyResponses) {
				assertEquals(TaskTestComponent.MULTI_ACTION1_NAME, response.action);
				assertTrue(response.success);
				assertNotNull(response.output);
				assertEquals("MultiAction1Output", response.output.getValue());
			}
			request.input.setValue("MultiAction2Input");
			return true;

		}

		@Override
		public MultiTaskOutput finish(Set<TaskActionResponse<MultiTaskOutput>> actions, MultiTaskOutput output) {
			assertEquals(3, actions.size());
			for (TaskActionResponse<MultiTaskOutput> response : actions) {
				assertTrue(response.success);
				assertNotNull(response.output);
				if (TaskTestComponent.MULTI_ACTION1_NAME.equals(response.action)) {
					assertEquals("MultiAction1Output", response.output.getValue());
				} else if (TaskTestComponent.MULTI_ACTION2_NAME.equals(response.action)) {
					assertEquals("MultiAction2Output", response.output.getValue());
				}
			}
			output = new MultiTaskOutput();
			output.setValue("MultiTaskOutput");
			return output;
		}

		@Override
		public QName name() {
			return TaskTestComponent.MULTI_TASK_COORD_NAME;
		}

		@Override
		public Set<QName> dependencies() {
			return Collections.EMPTY_SET;
		}

	}

	public static class MultiTaskActionExec implements TaskActionExec<MultiTaskInput, MultiTaskOutput> {
		TaskActionContext ctx;
		MultiTaskOutput out = new MultiTaskOutput();

		@Override
		public void start(TaskActionContext ctx, MultiTaskInput input) {
			this.ctx = ctx;
			ctx.log(LogLevel.INFO, 1, "start");
			if (TaskTestComponent.MULTI_ACTION1_NAME.equals(ctx.name())) {
				assertEquals("MultiAction1Input", input.getValue());
			} else if (TaskTestComponent.MULTI_ACTION2_NAME.equals(ctx.name())) {
				assertEquals("MultiAction2Input", input.getValue());

			}
		}

		@Override
		public void execute() {
			ctx.log(LogLevel.INFO, 2, "execute");
			if (TaskTestComponent.MULTI_ACTION1_NAME.equals(ctx.name())) {
				out.setValue("MultiAction1Output");
			} else if (TaskTestComponent.MULTI_ACTION2_NAME.equals(ctx.name())) {
				out.setValue("MultiAction2Output");

			}
		}

		@Override
		public MultiTaskOutput finish() {
			ctx.log(LogLevel.INFO, 3, "finish");
			return out;
		}

	}
}
