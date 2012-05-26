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
		Set<TaskActionRequest> init(Document input);

		void update(TaskActionRequest request, Set<TaskActionResponse> dependencyResponses);

		Document finish(Set<TaskActionResponse> actions);

	}

	public static class TaskActionRequest {
		final TaskActionDefinition definition;
		final Document input;

		public TaskActionRequest(TaskActionDefinition definition, Document input) {
			this.definition = definition;
			this.input = input;
		}

	}

	public static class TaskActionResponse {
		final TaskActionDefinition definition;
		final Document result;
		final String nodeId;

		public TaskActionResponse(TaskActionDefinition definition, String nodeId, Document result) {
			this.definition = definition;
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

		public TaskDefinition(QName name) {
			this.name = name;
			this.coordinators = new HashSet<TaskActionCoordinator>();
		}

		public QName getName() {
			return name;
		}

		public void addTaskActionCoordinator(TaskActionCoordinator coordinator) {
			synchronized (coordinators) {
				coordinators.add(coordinator);
			}
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof TaskDefinition) {
				TaskDefinition a2 = (TaskDefinition) o;
				if (name.equals(a2.getName())) {
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

		public TaskActionDefinition(QName name, TaskActionType type, Set<QName> dependencies) {
			this.name = name;
			this.type = type;
			this.dependencies = dependencies;
		}

		QName action() {
			return name;
		}

		TaskActionType type() {
			return type;
		}

		public Set<QName> dependencies() {
			return dependencies;
		}
	}

	public static interface TaskActionId {
		
	}

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

		public void start(TaskActionContext ctx) throws PlatformException;

		public void run(TaskActionContext ctx) throws PlatformException;

		public void finish(TaskActionContext ctx) throws PlatformException;

		@Override
		public boolean equals(Object o);


	}
	
	public static interface TaskActionContext {

		public TaskActionId id();

		public QName name();

		public void log(LogLevel level, int code, String message);

		public Document input();

		public TaskActionState getState();

		public void updateState(TaskActionState state) throws PlatformException;

		//public void coordinate(Document result) throws PlatformException;

		public void updateResult(Document result) throws PlatformException;
	}

}