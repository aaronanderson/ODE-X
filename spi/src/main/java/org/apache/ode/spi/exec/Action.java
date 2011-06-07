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
import static org.apache.ode.spi.exec.Platform.PLATFORM_NAMESPACE;

public class Action {

	public static final Action INSTALL_ACTION = new Action(new QName(PLATFORM_NAMESPACE, "install"), new String[] { "INSTALL", "INSTALLING", "INSTALLED" });
	public static final Action START_ACTION = new Action(new QName(PLATFORM_NAMESPACE, "start"), new String[] { "START", "STARTING", "STARTED" });
	public static final Action STOP_ACTION = new Action(new QName(PLATFORM_NAMESPACE, "stop"), new String[] { "STOP", "STOPING", "STOPPED" });
	public static final Action UNINSTALL_ACTION = new Action(new QName(PLATFORM_NAMESPACE, "uninstall"), new String[] { "UNINSTALL", "UNINSTALLING",
			"UNINSTALLED" });

	final QName type;
	final String[] states;

	public Action(QName type, String[] states) {
		this.type = type;
		this.states = states;
	}

	public QName getActionType() {
		return type;
	}

	public String[] getStates() {
		return states;
	}

	public static interface ActionId {
	}

}
