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
package org.apache.ode.spi.exec.task;

import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.ode.spi.exec.target.Target;

public interface TaskActionCoordinator<TI, I, O, TO> {
	//all will be called on master

	QName name();

	Set<QName> dependencies();

	Set<TaskActionRequest<I>> init(TaskContext ctx, TI input, String localNodeId, TaskCallback<?, ?> callback, Target... targets);

	//void refresh(TaskActionResponse<O> action);

	boolean update(TaskActionRequest<I> request, Set<TaskActionResponse<O>> dependencyResponses);

	TO finish(Set<TaskActionResponse<O>> actions, TO output);

	public static class TaskActionRequest<I> {
		public final QName action;
		public final String nodeId;
		public final I input;

		public TaskActionRequest(QName action, String nodeId, I input) {
			this.action = action;
			this.input = input;
			this.nodeId = nodeId;
		}

	}

	public static class TaskActionResponse<O> {
		public final QName action;
		public final O output;
		public final String nodeId;
		public final boolean success;

		public TaskActionResponse(QName action, String nodeId, O output, boolean success) {
			this.action = action;
			this.output = output;
			this.nodeId = nodeId;
			this.success = success;
		}

	}

}