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

import javax.inject.Provider;
import javax.xml.namespace.QName;

import org.apache.ode.spi.exec.ActionTask.ActionContext;

public class Action {

	private final QName name;
	private final TaskType type;
	private final Provider<? extends ActionTask<?>> provider;

	public Action(QName name, TaskType type, Provider<? extends ActionTask<?>> provider) {
		this.name = name;
		this.type = type;
		this.provider = provider;
	}

	public QName getName() {
		return name;
	}

	public TaskType getType() {
		return type;
	}
	
	public Provider<? extends ActionTask<?>> getProvider() {
		return provider;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Action) {
			Action a2 = (Action) o;
			if (name.equals(a2.getName()) && type.equals(a2.getType())) {
				return true;
			}
		}
		return false;
	}

	public static enum TaskType {
		ACTION, MASTER, SLAVE, SINGLE_SLAVE
	}
}