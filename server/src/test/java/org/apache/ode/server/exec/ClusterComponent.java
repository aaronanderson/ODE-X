/*
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
package org.apache.ode.server.exec;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.ode.spi.exec.Action;
import org.apache.ode.spi.exec.ActionTask;
import org.apache.ode.spi.exec.ActionTask.ActionContext;
import org.apache.ode.spi.exec.Component;
import org.apache.ode.spi.exec.MasterActionTask;
import org.apache.ode.spi.exec.Platform;
import org.apache.ode.spi.exec.PlatformException;
import org.apache.ode.spi.exec.SlaveActionTask;
import org.apache.ode.spi.exec.SlaveActionTask.SlaveActionStatus;
import org.apache.ode.spi.repo.Repository;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Singleton
public class ClusterComponent implements Component {
	public static final String TEST_NS = "http://ode.apache.org/ClusterTest";
	public static final QName COMPONENT_NAME = new QName(TEST_NS, "TestComponent");
	public static final QName TEST_LOCAL_ACTION = new QName(TEST_NS, "TestLocalAction");
	public static final QName TEST_REMOTE_ACTION = new QName(TEST_NS, "TestRemoteAction");
	public static final QName TEST_MASTER_SLAVE_ACTION = new QName(TEST_NS, "TestMasterAction");

	List<Action> supportedActions = new ArrayList<Action>();

	@Inject
	Repository repository;

	@Inject
	Platform platform;

	@PostConstruct
	public void init() {
		System.out.println("Initializing ClusterComponent");
		platform.registerComponent(this);
		System.out.println("ClusterComponent Initialized");

	}

	@Override
	public QName name() {
		return COMPONENT_NAME;
	}

	@Override
	public List<InstructionSet> instructionSets() {
		List<InstructionSet> instructions = new ArrayList<InstructionSet>();
		return instructions;
	}

	@Override
	public List<Action> actions() {
		return supportedActions;
	}

	public interface ExecCallback {
		public void execute(ActionContext context) throws PlatformException;
	}

	@Override
	public void online() throws PlatformException {

	}

	@Override
	public void offline() throws PlatformException {

	}

	public static Document testActionDoc(String input) {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.newDocument();
			Element root = doc.createElementNS(TEST_NS, "action-config");
			root.setTextContent(input);
			doc.appendChild(root);
			return doc;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	public static class LocalTestAction implements ActionTask<ActionContext> {

		static final CyclicBarrier notify = new CyclicBarrier(2);

		public static Provider<LocalTestAction> getProvider() {
			return new Provider<LocalTestAction>() {

				@Override
				public LocalTestAction get() {
					return new LocalTestAction();
				}

			};

		}

		static void notify(int time, TimeUnit unit) throws Exception {
			notify.await(time, unit);
		}

		@Override
		public void start(ActionContext ctx) throws PlatformException {
			try {
				notify.await();
				// System.out.println("Status " + ctx.getStatus());
				notify.await();
			} catch (Exception e) {
			}
		}

		@Override
		public void run(ActionContext ctx) {
			try {
				notify.await(2, TimeUnit.SECONDS);
				// System.out.println("Status " + ctx.getStatus());
				notify.await(2, TimeUnit.SECONDS);
			} catch (Exception e) {
			}
		}

		@Override
		public void finish(ActionContext ctx) throws PlatformException {
			try {
				notify.await(2, TimeUnit.SECONDS);
				// System.out.println("Status " + ctx.getStatus());
				notify.await(2, TimeUnit.SECONDS);
			} catch (Exception e) {
			}
		}

	}

	public static class RemoteTestAction implements ActionTask<ActionContext> {
		String content = null;

		public static Provider<RemoteTestAction> getProvider() {
			return new Provider<RemoteTestAction>() {

				@Override
				public RemoteTestAction get() {
					return new RemoteTestAction();
				}

			};

		}

		@Override
		public void start(ActionContext ctx) throws PlatformException {
			content = ctx.input().getDocumentElement().getTextContent();
			content += " start";
		}

		@Override
		public void run(ActionContext ctx) {
			content += " run";
		}

		@Override
		public void finish(ActionContext ctx) throws PlatformException {
			content += " finish";
			ctx.updateResult(testActionDoc(content));
		}

	}

	public static class MasterTestAction implements MasterActionTask {
		String content = null;

		public static Provider<MasterTestAction> getProvider() {
			return new Provider<MasterTestAction>() {

				@Override
				public MasterTestAction get() {
					return new MasterTestAction();
				}

			};

		}

		@Override
		public void start(MasterActionContext ctx) throws PlatformException {
			content = ctx.input().getDocumentElement().getTextContent();
			content += " start master";
			for (ActionStatus s : ctx.slaveStatus()) {
				ctx.updateInput(s.nodeId(), testActionDoc("input master"));
			}
		}

		@Override
		public void run(MasterActionContext ctx) throws PlatformException {
			content += " run master";
			for (ActionStatus s : ctx.slaveStatus()) {
				content += " update master";
				ctx.updateInput(s.nodeId(), testActionDoc("update master"));
			}

			boolean finished = false;
			do {
				ctx.refresh();
				for (ActionStatus s : ctx.slaveStatus()) {
					if (!ActionState.EXECUTING.equals(s.state())) {
						finished = true;
					} else if (s.result() != null && "update slave".equals(s.result().getDocumentElement().getTextContent())) {
						content += " " + s.result().getDocumentElement().getTextContent();
						finished = true;
					}
				}
			} while (!finished);
		}

		@Override
		public void finish(MasterActionContext ctx) throws PlatformException {
			for (ActionStatus s : ctx.slaveStatus()) {
				content += " " + s.result().getDocumentElement().getTextContent();
			}
			content += " finish master";
			ctx.updateResult(testActionDoc(content));
		}

	}

	public static class SlaveTestAction implements SlaveActionTask {
		String content = null;

		public static Provider<SlaveTestAction> getProvider() {
			return new Provider<SlaveTestAction>() {

				@Override
				public SlaveTestAction get() {
					return new SlaveTestAction();
				}

			};

		}

		@Override
		public void start(SlaveActionContext ctx) throws PlatformException {
			content = ctx.input().getDocumentElement().getTextContent();
			content += " start slave";
		}

		@Override
		public void run(SlaveActionContext ctx) throws PlatformException {
			content += " run slave";

			while (ActionState.EXECUTING.equals(ctx.masterStatus().state())) {
				if ("update master".equals(ctx.input().getDocumentElement().getTextContent())) {
					content += " update slave";
					ctx.updateResult(testActionDoc("update slave"));
					break;
				}
				ctx.refresh();
			}
		}

		@Override
		public void finish(SlaveActionContext ctx) throws PlatformException {
			content += " finish slave";
			ctx.updateResult(testActionDoc(content));
		}

	}

}
