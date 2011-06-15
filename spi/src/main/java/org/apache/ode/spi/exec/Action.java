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

import javax.xml.namespace.QName;

import org.apache.ode.spi.exec.Platform.PlatformAction;

public class Action {

	public static final Action INSTALL_ACTION = new Action(PlatformAction.INSTALL_ACTION.qname(), TaskType.SLAVE);
	public static final Action UNINSTALL_ACTION = new Action(PlatformAction.UNINSTALL_ACTION.qname(), TaskType.SLAVE);
	public static final Action START_ACTION = new Action(PlatformAction.START_ACTION.qname(), TaskType.SLAVE);
	public static final Action STOP_ACTION = new Action(PlatformAction.STOP_ACTION.qname(), TaskType.SLAVE);

	private final QName qname;
	private final TaskType type;

	public Action(QName qname, TaskType type) {
		this.qname = qname;
		this.type = type;
	}

	public QName getQName() {
		return qname;
	}

	public TaskType getType() {
		return type;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Action) {
			Action a2 = (Action) o;
			if (qname.equals(a2.getQName()) && type.equals(a2.getType())) {
				return true;
			}
		}
		return false;
	}

	public static enum TaskType {
		ACTION, MASTER, SLAVE
	}
}