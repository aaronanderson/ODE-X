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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;

import javax.inject.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;

public class TaskActionDefinition<I, O> {
	final QName name;
	final Set<QName> dependencies;
	final Provider<? extends TaskActionActivity<I, O>> actionExec;
	private final IOBuilder<I, O> ioBuilder;
	final JAXBContext jaxbContext;
	boolean isTransactional;

	public TaskActionDefinition(QName name, Set<QName> dependencies, Provider<? extends TaskActionActivity<I, O>> actionExec, IOBuilder<I, O> ioFactory,
			JAXBContext jaxbContext) {
		this.name = name;
		this.dependencies = dependencies;
		this.actionExec = actionExec;
		this.ioBuilder = ioFactory;
		this.jaxbContext = jaxbContext;
		boolean isTransactional = false;
		for (Type t : actionExec.getClass().getGenericInterfaces()) {
			if (t instanceof Provider) {
				Type providerType = ((ParameterizedType) t).getActualTypeArguments()[0];
				if (providerType instanceof TaskActionTransaction) {
					isTransactional = true;
				}
				break;
			}
		}
		this.isTransactional = isTransactional;
	}

	public QName action() {
		return name;
	}

	public IOBuilder<I, O> ioBuilder() {
		return ioBuilder;

	}

	public JAXBContext jaxbContext() {
		return jaxbContext;
	}

	public Set<QName> dependencies() {
		return dependencies;
	}

	public boolean isTransactional() {
		return isTransactional;
	}

	public Provider<? extends TaskActionActivity<I, O>> actionExec() {
		return actionExec;
	}

}