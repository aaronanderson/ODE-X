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
package org.apache.ode.spi.exec;

import java.util.Date;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.ode.spi.exec.ActionTask.ActionContext;
import org.w3c.dom.Document;

/**
 * 
 * Intended to run once only on a single node
 * 
 */
public interface ActionTask<C extends ActionContext> extends Runnable {

	// returns QName of NodeActionTask\
	public void start(C ctx) throws PlatformException;

	@Override
	public void run();

	public void finish() throws PlatformException;

	public static interface ActionContext {

		public ActionId id();

		public QName type();

		public void log(ActionMessage message);

		public Document input();

		public void refresh();

		public Status getStatus();

		public void updateStatus(Status status);

		public void updateResult(Document result);
	}

	public static interface ActionStatus {

		public ActionId id();

		public QName type();

		public Status status();

		public List<ActionMessage> messages();

		public Document result();

		public Date start();

		public Date finish();

	}

	public static enum Status {
		START, EXECUTING, COMPLETED, PARTIAL_FAILURE, FAILED, CANCELED
	}

	public static interface ActionId {

	}

	public static class ActionMessage {
		private LogType type;
		private String message;

		public ActionMessage(LogType type, String message) {
			this.type = type;
			this.message = message;
		}

		public LogType getType() {
			return type;
		}

		public String getMessage() {
			return message;
		}

		public static enum LogType {
			INFO, WARNING, ERROR
		}
	}

}
