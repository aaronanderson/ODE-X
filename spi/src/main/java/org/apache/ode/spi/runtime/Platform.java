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
package org.apache.ode.spi.runtime;

import java.net.URI;

import javax.activation.CommandObject;
import javax.xml.namespace.QName;

import org.apache.ode.spi.exec.target.Target;
import org.apache.ode.spi.exec.task.TaskCallback;
import org.apache.ode.spi.repo.Artifact;
import org.apache.ode.spi.runtime.Component.EventSet;
import org.apache.ode.spi.runtime.Component.ExecutableSet;
import org.apache.ode.spi.runtime.Component.ExecutionConfigSet;
import org.apache.ode.spi.runtime.Component.ExecutionContextSet;
import org.w3c.dom.Document;

public interface Platform {

	public static final String EVENT_NAMESPACE = "http://ode.apache.org/event";
	public static final String EVENT_EXEC_NAMESPACE = "http://ode.apache.org/event-executable";
	public static final String EVENT_JUNCTION_NAMESPACE = "http://ode.apache.org/event-junction";
	public static final String EXEC_MIMETYPE = "application/ode-executable";
	public static final String EXEC_NAMESPACE = "http://ode.apache.org/executable";
	public static final String EXEC_CTX_NAMESPACE = "http://ode.apache.org/execution-context";
	public static final String EXEC_CFG_NAMESPACE = "http://ode.apache.org/execution-config";

	public static final String PLATFORM_NAMESPACE = "http://ode.apache.org/platform";
	public static final QName PLATFORM = new QName(PLATFORM_NAMESPACE, "platform");

	public static final String ARCHITECTURE_NAMESPACE = "http://ode.apache.org/architecture";

	public static final QName EXEC_INSTRUCTION_SET_NAME = new QName(EXEC_NAMESPACE, "Executable");
	public static final QName EXEC_CTX_SET_NAME = new QName(EXEC_CTX_NAMESPACE, "ExecContext");
	public static final QName EVENT_SET_NAME = new QName(EVENT_NAMESPACE, "Event");
	public static final QName EVENT_EXEC_SET_NAME = new QName(EVENT_EXEC_NAMESPACE, "ExecutableEvent");
	public static final QName EVENT_JUNCTION_SET_NAME = new QName(EVENT_JUNCTION_NAMESPACE, "ProgramEvent");
	public static final QName EXEC_CFG_SET_NAME = new QName(EXEC_CFG_NAMESPACE, "ExecConfig");

	public static final ExecutableSet EXEC_INSTRUCTION_SET = new ExecutableSet(EXEC_INSTRUCTION_SET_NAME, "org.apache.ode.spi.exec.executable.xml",
			org.apache.ode.spi.exec.executable.xml.ObjectFactory.class);
	public static final ExecutionContextSet EXEC_CTX_SET = new ExecutionContextSet(EXEC_CTX_SET_NAME, "org.apache.ode.spi.exec.instruction.xml",
			org.apache.ode.spi.exec.context.xml.ObjectFactory.class);
	public static final EventSet EVENT_SET = new EventSet(EVENT_SET_NAME, "org.apache.ode.spi.event.xml", org.apache.ode.spi.event.xml.ObjectFactory.class);
	public static final EventSet EVENT_EXEC_SET = new EventSet(EVENT_EXEC_SET_NAME, "org.apache.ode.spi.event.executable.xml",
			org.apache.ode.spi.event.executable.xml.ObjectFactory.class);
	public static final ExecutionConfigSet EXEC_CFG_SET = new ExecutionConfigSet(EXEC_CFG_SET_NAME, "org.apache.ode.spi.exec.config.xml",
			org.apache.ode.spi.exec.config.xml.ObjectFactory.class);

	public enum PlatformTask {

		SETUP_TASK(new QName(PLATFORM_NAMESPACE, "setup")), INSTALL_TASK(new QName(PLATFORM_NAMESPACE, "install")), ONLINE_TASK(new QName(PLATFORM_NAMESPACE, "online")), START_TASK(
				new QName(PLATFORM_NAMESPACE, "start")), STOP_TASK(new QName(PLATFORM_NAMESPACE, "stop")), OFFLINE_TASK(new QName(PLATFORM_NAMESPACE, "offline")), UNINSTALL_TASK(
				new QName(PLATFORM_NAMESPACE, "uninstall"));

		private PlatformTask(QName qname) {
			this.qname = qname;
		}

		private QName qname;

		public QName qname() {
			return qname;
		}
	}

	public interface PlatformTaskCommand extends CommandObject {
		public static final String PLATFORM_TASK_CMD = "platformTasks";

		public QName task(PlatformTask platformTask);

	}

	public static interface AuthContext extends TaskCallback<Document, Document> {

	}

	//public QName architecture(); 

	//public void registerComponent(Component component);

	//authentication

	public void login(AuthContext authContext) throws PlatformException;

	public void logout() throws PlatformException;

	//public void registerListener(MessageListener listener);

	//public void unregisterListener(MessageListener listener);

	//platform actions
	//public void setLogLevel(LogLevel level);

	//public Set<NodeStatus> status();
	public Execution execution(URI uri) throws PlatformException;

	//public <T extends Target> T createTarget(String id, Class<T> type) throws PlatformException;

	public void install(URI id, Artifact executionConfiguration, Target... targets) throws PlatformException;

	public void start(URI id, Target... targets) throws PlatformException;

	public void stop(URI id, Target... targets) throws PlatformException;

	public void uninstall(URI id, Target... targets) throws PlatformException;

	//junction
	//There are two ways external applications can interact with an Execution:
	// 1)At the install time of an Execution external junctions are bonded with it. All external events are directed through the junction
	// 2) The SPI call below is used to lookup the execution context, optionally override bonds with local instances, and then invoke events
	//    on the context
	//URIs must be absolute
	//public <E> Channel<E> channel(URI url);

	//public <E> Stream<E> stream(URI url, UUID context);

	//public Junction junction(URI uri, UUID context);

	//public <C> C proxy(Class<C> clazz, UUID context);
	//public ExecutionRuntime executionRuntime(UUID contextId);

	//task management
	/*
	public TaskId executeAsync(QName task, Document taskInput, TaskCallback<?, ?> callback, Target... targets) throws PlatformException;

	public Future<Document> execute(QName task, Document taskInput, TaskCallback<?, ?> callback, Target... targets) throws PlatformException;

	public Task taskStatus(TaskId taskId) throws PlatformException;

	public void cancel(TaskId taskId) throws PlatformException;

	public static interface NodeStatus {

		public String clusterId();

		public String nodeId();

		public NodeState state();

		public static enum NodeState {
			ONLINE, OFFLINE;
		}
	}*/

}
