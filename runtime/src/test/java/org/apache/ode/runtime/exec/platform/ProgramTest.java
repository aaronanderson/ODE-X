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

public class ProgramTest {
	private static final Logger log = Logger.getLogger(ProgramTest.class.getName());
	private static Jsr250Injector injector1;
	private static Jsr250Injector injector2;
	private static JAXBContext programJAXBContext;

	private static Node node1;
	private static Node node2;
	private static Platform platform1;
	private static Platform platform2;

	private static final org.apache.ode.runtime.exec.task.test.xml.ObjectFactory programObjectFactory = new org.apache.ode.runtime.exec.task.test.xml.ObjectFactory();

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		programJAXBContext = JAXBContext.newInstance("org.apache.ode.runtime.exec.cluster.xml:org.apache.ode.runtime.exec.task.test.xml");

		injector1 = Jsr250.createInjector(new VMServerModule("vm://task?broker.persistent=true&broker.useJmx=false&broker.dataDirectory=target/activemq-data",
				"pcluster", "node1"), new ProgramTestModule());
		HealthCheckTest.loadTestClusterConfig(injector1, "pcluster");
		injector2 = Jsr250.createInjector(new VMServerModule("vm://task?broker.persistent=true&broker.useJmx=false&broker.dataDirectory=target/activemq-data",
				"pcluster", "node2"), new ProgramTestModule());

		node1 = injector1.getInstance(Node.class);
		assertNotNull(node1);
		node1.online();
		ProgramTestComponent component1 = injector1.getInstance(ProgramTestComponent.class);
		assertNotNull(component1);
		platform1 = injector1.getInstance(Platform.class);
		assertNotNull(platform1);

		node2 = injector2.getInstance(Node.class);
		assertNotNull(node2);
		node2.online();
		ProgramTestComponent component2 = injector2.getInstance(ProgramTestComponent.class);
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

	public static class ProgramTestModule extends AbstractModule {

		@Override
		protected void configure() {
			bind(ProgramTestComponent.class);
			bind(SetupTaskActionExec.class);
		}

	}

	public void setup() {
	}

	public void install() {
	}

	public void start() {
	}

	public void stop() {
	}

	public void uninstall() {
	}

	//@Test
	public void setupTest() throws Exception {

	}

	//@Test
	public void installTest() throws Exception {

	}

	//@Test
	public void startTest() throws Exception {

	}

	//@Test
	public void stopTest() throws Exception {

	}

	//@Test
	public void uninstallTest() throws Exception {

	}

	//@Test
	public void newProcessTest() throws Exception {

	}

	//@Test
	public void newThreadTest() throws Exception {

	}

	//@Test
	public void syncExchangeTest() throws Exception {

	}

	//@Test
	public void aSyncExchangeTest() throws Exception {

	}

	public static class ProgramTestComponent implements Component {

		public static final String TEST_NS = "http://ode.apache.org/ProgramTest";
		public static final QName COMPONENT_NAME = new QName(TEST_NS, "ProgramComponent");

		public static final QName SETUP_TASK_COORD_NAME = new QName(TEST_NS, "SetupTaskCoordinator");

		public static final QName SETUP_ACTION_NAME = new QName(TEST_NS, "SetupTaskAction");

		@Inject
		Node node;

		@Inject
		Provider<SetupTaskActionExec> setupAction;

		@PostConstruct
		public void init() {
			log.fine("Initializing ProgramTestComponent");
			node.registerComponent(this);
			log.fine("ProgramTestComponent Initialized");

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
			return Collections.EMPTY_LIST;
		}

		@Override
		public List<TaskActionDefinition> actions() {
			ArrayList<TaskActionDefinition> defs = new ArrayList<TaskActionDefinition>();
			defs.add(new TaskActionDefinition(SETUP_ACTION_NAME, (Set<QName>) Collections.EMPTY_SET, setupAction, programJAXBContext));
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

	public static class SetupTaskActionCoordinator implements TaskActionCoordinator<SingleTaskInput, SingleTaskInput, SingleTaskOutput, SingleTaskOutput> {

		@Override
		public Set<TaskActionRequest<SingleTaskInput>> init(TaskContext ctx, SingleTaskInput input, String localNodeId, TaskCallback<?, ?> callback,
				Target... targets) {
			assertEquals("SingleTaskInput", input.getValue());
			input.setValue("SingleActionInput");
			String[] nodeIds = TaskDefinition.targetsToNodeIds(targets);
			HashSet<TaskActionRequest<SingleTaskInput>> actions = new HashSet<TaskActionRequest<SingleTaskInput>>();
			TaskActionRequest<SingleTaskInput> req = new TaskActionRequest<SingleTaskInput>(ProgramTestComponent.SETUP_ACTION_NAME, nodeIds[0], input);
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
			assertEquals(ProgramTestComponent.SETUP_ACTION_NAME, response.action);
			assertTrue(response.success);
			assertNotNull(response.output);
			assertEquals("SingleActionOutput", response.output.getValue());
			output = new SingleTaskOutput();
			output.setValue("SingleTaskOutput");
			return output;
		}

		@Override
		public QName name() {
			return ProgramTestComponent.SETUP_TASK_COORD_NAME;
		}

		@Override
		public Set<QName> dependencies() {
			return Collections.EMPTY_SET;
		}

	}

	public static class SetupTaskActionExec implements TaskActionExec<MultiTaskInput, MultiTaskOutput> {
		TaskActionContext ctx;
		MultiTaskOutput out = new MultiTaskOutput();

		@Override
		public void start(TaskActionContext ctx, MultiTaskInput input) {
			this.ctx = ctx;
			ctx.log(LogLevel.INFO, 1, "start");
			if (ProgramTestComponent.SETUP_ACTION_NAME.equals(ctx.name())) {
				assertEquals("MultiAction1Input", input.getValue());
			}
		}

		@Override
		public void execute() {
			ctx.log(LogLevel.INFO, 2, "execute");
			if (ProgramTestComponent.SETUP_ACTION_NAME.equals(ctx.name())) {
				out.setValue("MultiAction1Output");
			}
		}

		@Override
		public MultiTaskOutput finish() {
			ctx.log(LogLevel.INFO, 3, "finish");
			return out;
		}

	}
}
