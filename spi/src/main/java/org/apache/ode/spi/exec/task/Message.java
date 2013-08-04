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

import java.util.Date;

public interface Message {

	public Date timestamp();

	public LogLevel level();

	public int code();

	public String message();

	public static enum LogLevel {
		DEBUG, INFO, WARNING, ERROR
	}

	public static interface MessageEvent {

		public Message message();

		public String clusterId();

		public String nodeId();

	}

	public static interface MessageListener {

		public LogLevel levelFilter();

		public void message(MessageEvent message);

	}

	public static interface TaskListener extends MessageListener {

	}

	public static interface ClusterListener extends MessageListener {

		public String clusterIdFilter();

	}

	public static interface NodeListener extends MessageListener {

		public String nodeIdFilter();

	}

}