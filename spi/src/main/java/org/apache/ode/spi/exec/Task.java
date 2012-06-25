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

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Provider;
import javax.xml.namespace.QName;

import org.apache.ode.spi.exec.Message.LogLevel;
import org.apache.ode.spi.exec.Message.MessageEvent;
import org.apache.ode.spi.exec.Message.MessageListener;
import org.w3c.dom.Document;

public interface Task {

	public static enum TaskState {
		SUBMIT, START, EXECUTE, CANCEL, FINISH, COMPLETE, FAIL
	}

	public static interface TaskId {

	}

	public String nodeId();

	public void refresh();

	public QName name();

	public TaskId id();

	public QName component();

	public TaskState state();

	public Set<Target> targets();

	public List<Message> messages();

	public Set<TaskAction> actions();

	public Document input();

	public Document result();

	public Date start();

	public Date finish();

	public Date modified();

	public static interface TaskActionCoordinator {
		//all will be called on master
		Set<TaskActionRequest> init(Document input, Target... targets);

		void update(TaskActionRequest request, Set<TaskActionResponse> dependencyResponses);

		Document finish(Set<TaskActionResponse> actions);

	}

	public static class TaskActionRequest {
		public final QName action;
		public final Document input;

		public TaskActionRequest(QName action, Document input) {
			this.action = action;
			this.input = input;
		}

	}

	public static class TaskActionResponse {
		public final QName action;
		public final Document result;
		public final String nodeId;

		public TaskActionResponse(QName action, String nodeId, Document result) {
			this.action = action;
			this.result = result;
			this.nodeId = nodeId;
		}

	}

	public static enum TaskActionType {
		SINGLE, MULTIPLE;
	}

	public static enum TaskActionState {
		SUBMIT, START, EXECUTE, FINISH, ROLLBACK, COMMIT, COMPLETE
	}

	public class TaskDefinition {

		private final QName name;
		private final Set<TaskActionCoordinator> coordinators;

		public TaskDefinition(QName name, TaskActionCoordinator coordinator) {
			this.name = name;
			this.coordinators = new HashSet<TaskActionCoordinator>();
			coordinators.add(coordinator);
		}

		public QName task() {
			return name;
		}

		public void addTaskActionCoordinator(TaskActionCoordinator coordinator) {
			synchronized (coordinators) {
				coordinators.add(coordinator);
			}
		}

		public Set<TaskActionCoordinator> coordinators() {
			return coordinators;
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof TaskDefinition) {
				TaskDefinition a2 = (TaskDefinition) o;
				if (name.equals(a2.task())) {
					return true;
				}
			}
			return false;
		}
	}

	public static class TaskActionDefinition {
		final QName name;
		final Set<QName> dependencies;
		final TaskActionType type;
		final Provider<? extends TaskActionExec> actionExec;
		final boolean interactive;

		public TaskActionDefinition(QName name, TaskActionType type, Set<QName> dependencies, Provider<? extends TaskActionExec> actionExec, boolean interactive) {
			this.name = name;
			this.type = type;
			this.dependencies = dependencies;
			this.actionExec = actionExec;
			this.interactive = interactive;
		}

		public QName action() {
			return name;
		}

		public boolean interactive() {
			return interactive;
		}

		public TaskActionType type() {
			return type;
		}

		public Set<QName> dependencies() {
			return dependencies;
		}

		public Provider<? extends TaskActionExec> actionExec() {
			return actionExec;
		}
	}

	public static interface TaskActionId {

	}

	//TODO support external TaskActions, where action actually performed outside ODE framework but action updates and messages stil managed by ODE

	public static interface TaskAction {

		public void refresh();

		public QName name();

		public TaskActionId id();

		public String nodeId();

		public QName component();

		public TaskActionState state();

		public List<Message> messages();

		public Document input();

		public Document result();

		public Date start();

		public Date finish();

		public Date modified();

	}

	public static interface TaskMessageEvent extends MessageEvent {

		public String taskId();

		public QName task();

	}

	public static interface TaskActionMessageEvent extends TaskMessageEvent {

		public String actionId();

		public QName action();

	}

	public static interface TaskMessageListener extends MessageListener {

	}

	public interface TaskActionExec {

		public void start(TaskActionContext ctx, Document input) throws PlatformException;

		public Document execute(TaskActionContext ctx, Document coordination) throws PlatformException;

		public Document finish(TaskActionContext ctx) throws PlatformException;

		@Override
		public boolean equals(Object o);

	}

	public static interface TaskActionContext {

		public TaskActionId id();

		public QName name();

		public void log(LogLevel level, int code, String message);

		public TaskActionState getState();

		public void updateState(TaskActionState state) throws PlatformException;
		
		public boolean interactive();

		//public void coordinate(Document result) throws PlatformException;
	}

}