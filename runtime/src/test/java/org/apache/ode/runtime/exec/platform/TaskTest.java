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

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.jms.Queue;
import javax.jms.Topic;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.ode.runtime.exec.modules.JPAModule;
import org.apache.ode.runtime.exec.modules.NodeModule;
import org.apache.ode.runtime.exec.modules.RepoModule;
import org.apache.ode.runtime.exec.modules.AQJMSModule.AQBroker;
import org.apache.ode.runtime.exec.modules.AQJMSModule.AQBrokerURL;
import org.apache.ode.runtime.exec.modules.AQJMSModule.AQJMSTypeListener;
import org.apache.ode.runtime.exec.modules.AQJMSModule.VMAQBroker;
import org.apache.ode.runtime.exec.platform.HealthCheckTest.HealthCheckTestModule;
import org.apache.ode.runtime.exec.platform.JMSUtil.QueueImpl;
import org.apache.ode.runtime.exec.platform.JMSUtil.TopicImpl;
import org.apache.ode.runtime.exec.platform.TargetImpl.TargetAllImpl;
import org.apache.ode.spi.exec.Component;
import org.apache.ode.spi.exec.Message.LogLevel;
import org.apache.ode.spi.exec.Message.MessageEvent;
import org.apache.ode.spi.exec.Message.TaskListener;
import org.apache.ode.spi.exec.Node;
import org.apache.ode.spi.exec.Platform;
import org.apache.ode.spi.exec.PlatformException;
import org.apache.ode.spi.exec.Target;
import org.apache.ode.spi.exec.Task.TaskActionContext;
import org.apache.ode.spi.exec.Task.TaskActionCoordinator;
import org.apache.ode.spi.exec.Task.TaskActionDefinition;
import org.apache.ode.spi.exec.Task.TaskActionExec;
import org.apache.ode.spi.exec.Task.TaskActionRequest;
import org.apache.ode.spi.exec.Task.TaskActionResponse;
import org.apache.ode.spi.exec.Task.TaskActionType;
import org.apache.ode.spi.exec.Task.TaskDefinition;
import org.apache.ode.spi.exec.Task.TaskId;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.inject.matcher.Matchers;
import com.mycila.inject.jsr250.Jsr250;
import com.mycila.inject.jsr250.Jsr250Injector;

public class TaskTest {
	private static final Logger log = Logger.getLogger(TaskTest.class.getName());
	private static Jsr250Injector injector1;
	private static Jsr250Injector injector2;

	private static Topic healthCheckTopic = new TopicImpl(Node.NODE_MQ_NAME_HEALTHCHECK);
	private static Queue taskQueue = new QueueImpl(Node.NODE_MQ_NAME_TASK);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		injector1 = Jsr250.createInjector(new JPAModule(), new RepoModule(), new NodeModule(), new TaskTestModule(
				"vm://task?broker.persistent=true&broker.useJmx=false&broker.dataDirectory=target/activemq-data", "tcluster", "node1"));
		HealthCheckTest.loadTestClusterConfig(injector1, "tcluster");
		injector2 = Jsr250.createInjector(new JPAModule(), new RepoModule(), new NodeModule(), new TaskTestModule(
				"vm://task?broker.persistent=true&broker.useJmx=false&broker.dataDirectory=target/activemq-data", "tcluster", "node2"));

		Node node1 = injector1.getInstance(Node.class);
		assertNotNull(node1);
		node1.online();

		Node node2 = injector2.getInstance(Node.class);
		assertNotNull(node2);
		node2.online();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		injector1.destroy();
		injector2.destroy();
	}

	public static class TaskTestModule extends HealthCheckTestModule {
		String clusterId;
		String nodeId;
		String aqBrokerURL;

		public TaskTestModule(String aqBrokerURL, String clusterId, String nodeId) {
			super(aqBrokerURL, clusterId, nodeId);
		}

		@Override
		protected void configure() {
			super.configure();

			bind(TaskTestComponent.class);
			bind(SingleTaskActionExec.class);
			bind(MultiTaskActionExec.class);
		}

	}

	@Test
	public void nonInteractiveTaskActionTest() throws Exception {

	}
	
	@Test
	public void interactiveTaskActionTest() throws Exception {

	}

	/*
	@Test
	public void singleTaskTest() throws Exception {

		Platform platform1 = injector1.getInstance(Platform.class);
		assertNotNull(platform1);

		final Set<Integer> codes = new HashSet<Integer>();

		platform1.registerListener(new TaskListener() {

			@Override
			public LogLevel levelFilter() {
				return LogLevel.DEBUG;
			}

			@Override
			public void message(MessageEvent message) {
				log.info(message.toString());
				codes.add(message.message().code());
			}

		});
		TaskId id = platform1.execute(TaskTestComponent.SINGLE_TASK_NAME, testDoc("task-config", "TestTaskInput"), new TargetAllImpl());
		assertNotNull(id);
		//platform.status(id);

	}*/

	public static class TaskTestComponent implements Component {

		public static final String TEST_NS = "http://ode.apache.org/TaskTest";
		public static final QName COMPONENT_NAME = new QName(TEST_NS, "TaskComponent");

		public static final QName SINGLE_TASK_NAME = new QName(TEST_NS, "SingleTask");
		public static final QName MULTI_TASK_NAME = new QName(TEST_NS, "MultiTask");

		public static final QName SINGLE_ACTION_NAME = new QName(TEST_NS, "SingleTaskAction");
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
			defs.add(new TaskDefinition(SINGLE_TASK_NAME, new SingleTaskActionCoordinator()));
			defs.add(new TaskDefinition(MULTI_TASK_NAME, new MultiTaskActionCoordinator()));
			return defs;
		}

		@Override
		public List<TaskActionDefinition> actions() {
			ArrayList<TaskActionDefinition> defs = new ArrayList<TaskActionDefinition>();
			defs.add(new TaskActionDefinition(SINGLE_ACTION_NAME, TaskActionType.SINGLE, (Set<QName>) Collections.EMPTY_SET, singleAction, false));
			defs.add(new TaskActionDefinition(MULTI_ACTION1_NAME, TaskActionType.SINGLE, (Set<QName>) Collections.EMPTY_SET, multiAction, false));
			Set<QName> deps = new HashSet<QName>();
			deps.add(MULTI_ACTION1_NAME);
			defs.add(new TaskActionDefinition(MULTI_ACTION2_NAME, TaskActionType.MULTIPLE, deps, multiAction, false));
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

	public static class SingleTaskActionCoordinator implements TaskActionCoordinator {

		@Override
		public Set<TaskActionRequest> init(Document input, Target... targets) {
			HashSet<TaskActionRequest> actions = new HashSet<TaskActionRequest>();
			TaskActionRequest req = new TaskActionRequest(TaskTestComponent.SINGLE_ACTION_NAME, testDoc("task-action-config", input.getDocumentElement()
					.getTextContent()));
			actions.add(req);
			return actions;
		}

		@Override
		public void update(TaskActionRequest request, Set<TaskActionResponse> dependencyResponses) {

		}

		@Override
		public Document finish(Set<TaskActionResponse> actions) {
			return null;
		}

	}

	public static class SingleTaskActionExec implements TaskActionExec {

		@Override
		public void start(TaskActionContext ctx, Document input) throws PlatformException {
			ctx.log(LogLevel.INFO, 1, "start");
		}

		@Override
		public Document execute(TaskActionContext ctx, Document coordination) throws PlatformException {
			ctx.log(LogLevel.INFO, 1, "run");
			return null;
		}

		@Override
		public Document finish(TaskActionContext ctx) throws PlatformException {
			ctx.log(LogLevel.INFO, 1, "finish");
			return null;
		}

	}

	public static class MultiTaskActionCoordinator implements TaskActionCoordinator {

		@Override
		public Set<TaskActionRequest> init(Document input, Target... targets) {
			HashSet<TaskActionRequest> actions = new HashSet<TaskActionRequest>();
			TaskActionRequest req = new TaskActionRequest(TaskTestComponent.MULTI_ACTION1_NAME, testDoc("task-action-config", input.getDocumentElement()
					.getTextContent()));
			actions.add(req);
			req = new TaskActionRequest(TaskTestComponent.MULTI_ACTION2_NAME, testDoc("task-action-config", input.getDocumentElement().getTextContent()));
			actions.add(req);
			return actions;
		}

		@Override
		public void update(TaskActionRequest request, Set<TaskActionResponse> dependencyResponses) {
			// TODO Auto-generated method stub

		}

		@Override
		public Document finish(Set<TaskActionResponse> actions) {
			// TODO Auto-generated method stub
			return null;
		}

	}

	public static class MultiTaskActionExec implements TaskActionExec {

		@Override
		public void start(TaskActionContext ctx, Document input) throws PlatformException {
			ctx.log(LogLevel.INFO, 1, "start");
		}

		@Override
		public Document execute(TaskActionContext ctx, Document coordination) throws PlatformException {
			ctx.log(LogLevel.INFO, 1, "run");
			return null;
		}

		@Override
		public Document finish(TaskActionContext ctx) throws PlatformException {
			ctx.log(LogLevel.INFO, 1, "finish");
			return null;
		}

	}
}
