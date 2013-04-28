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
package org.apache.ode.spi.exec;

import java.util.Set;
import java.util.concurrent.Future;

import javax.activation.CommandObject;
import javax.xml.namespace.QName;

import org.apache.ode.spi.exec.Component.EventSet;
import org.apache.ode.spi.exec.Component.ExecutionContextSet;
import org.apache.ode.spi.exec.Component.ExecutableSet;
import org.apache.ode.spi.exec.Component.ProgramSet;
import org.apache.ode.spi.exec.Message.LogLevel;
import org.apache.ode.spi.exec.Message.MessageListener;
import org.apache.ode.spi.exec.target.Target;
import org.apache.ode.spi.exec.task.Task;
import org.apache.ode.spi.exec.task.Task.TaskId;
import org.apache.ode.spi.exec.task.TaskCallback;
import org.apache.ode.spi.repo.Artifact;
import org.w3c.dom.Document;

public interface Platform {

	public static final String EVENT_NAMESPACE = "http://ode.apache.org/event";
	public static final String EVENT_EXEC_NAMESPACE = "http://ode.apache.org/event-executable";
	public static final String EVENT_PROGRAM_NAMESPACE = "http://ode.apache.org/event-program";
	public static final String EXEC_MIMETYPE = "application/ode-executable";
	public static final String EXEC_NAMESPACE = "http://ode.apache.org/executable";
	public static final String EXEC_CTX_NAMESPACE = "http://ode.apache.org/execution-context";
	public static final String PROGRAM_NAMESPACE = "http://ode.apache.org/program";

	public static final String PLATFORM_NAMESPACE = "http://ode.apache.org/platform";
	public static final QName PLATFORM = new QName(PLATFORM_NAMESPACE, "platform");

	public static final String ARCHITECTURE_NAMESPACE = "http://ode.apache.org/architecture";

	public static final QName EXEC_INSTRUCTION_SET_NAME = new QName(EXEC_NAMESPACE, "Executable");
	public static final QName EXEC_CTX_SET_NAME = new QName(EXEC_CTX_NAMESPACE, "ExecContext");
	public static final QName EVENT_SET_NAME = new QName(EVENT_NAMESPACE, "Event");
	public static final QName EVENT_EXEC_SET_NAME = new QName(EVENT_EXEC_NAMESPACE, "ExecutableEvent");
	public static final QName EVENT_PROGRAM_SET_NAME = new QName(EVENT_PROGRAM_NAMESPACE, "ProgramEvent");
	public static final QName PROGRAM_SET_NAME = new QName(PROGRAM_NAMESPACE, "Program");
	
	public static final ExecutableSet EXEC_INSTRUCTION_SET = new ExecutableSet(EXEC_INSTRUCTION_SET_NAME, "org.apache.ode.spi.exec.executable.xml",
			org.apache.ode.spi.exec.executable.xml.ObjectFactory.class, EXEC_CTX_SET_NAME, EVENT_EXEC_SET_NAME,PROGRAM_SET_NAME);
	public static final ExecutionContextSet EXEC_CTX_SET = new ExecutionContextSet(EXEC_CTX_SET_NAME, "org.apache.ode.spi.exec.instruction.xml",
			org.apache.ode.spi.exec.context.xml.ObjectFactory.class);
	public static final EventSet EVENT_SET = new EventSet(EVENT_SET_NAME, "org.apache.ode.spi.event.xml",
			org.apache.ode.spi.event.xml.ObjectFactory.class);
	public static final EventSet EVENT_EXEC_SET = new EventSet(EVENT_EXEC_SET_NAME, "org.apache.ode.spi.event.executable.xml",
			org.apache.ode.spi.event.executable.xml.ObjectFactory.class);
	public static final EventSet EVENT_PROGRAM_SET = new EventSet(EVENT_PROGRAM_SET_NAME, "org.apache.ode.spi.event.program.xml",
			org.apache.ode.spi.event.program.xml.ObjectFactory.class);
	public static final ProgramSet PROGRAM_SET = new ProgramSet(PROGRAM_SET_NAME, "org.apache.ode.spi.exec.program.xml",
			org.apache.ode.spi.exec.program.xml.ObjectFactory.class, EVENT_PROGRAM_SET_NAME);

	public enum PlatformTask {

		SETUP_TASK(new QName(PLATFORM_NAMESPACE, "setup")), INSTALL_TASK(new QName(PLATFORM_NAMESPACE, "install")), ONLINE_TASK(new QName(PLATFORM_NAMESPACE,
				"online")), START_TASK(new QName(PLATFORM_NAMESPACE, "start")), STOP_TASK(new QName(PLATFORM_NAMESPACE, "stop")), OFFLINE_TASK(new QName(
				PLATFORM_NAMESPACE, "offline")), UNINSTALL_TASK(new QName(PLATFORM_NAMESPACE, "uninstall"));

		private PlatformTask(QName qname) {
			this.qname = qname;
		}

		private QName qname;

		public QName qname() {
			return qname;
		}
	}

	public interface PlatformTaskCommand extends CommandObject{
		public static final String PLATFORM_TASK_CMD = "platformTasks";

		public QName task(PlatformTask platformTask);

	}

	public static interface AuthContext extends TaskCallback<Document, Document> {

	}

	//public QName architecture(); 

	//public void registerComponent(Component component);

	public void login(AuthContext authContext) throws PlatformException;

	public void logout() throws PlatformException;

	public void registerListener(MessageListener listener);

	public void unregisterListener(MessageListener listener);

	public void setLogLevel(LogLevel level);

	public Set<NodeStatus> status();

	public <T extends Target> T createTarget(String id, Class<T> type) throws PlatformException;

	public Document setup(Artifact... executables) throws PlatformException;

	public void install(QName id, Document programConfiguration, Target... targets) throws PlatformException;

	public Program programInfo(QName id) throws PlatformException;

	public void start(QName id, Target... targets) throws PlatformException;

	public void stop(QName id, Target... targets) throws PlatformException;

	public void uninstall(QName id, Target... targets) throws PlatformException;

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
	}

}
