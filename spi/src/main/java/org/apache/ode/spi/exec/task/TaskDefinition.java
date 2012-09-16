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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.ode.spi.exec.target.Target;

public class TaskDefinition<TI, TO> {

	private final QName name;
	private final Set<TaskActionCoordinatorDefinition<TI, TO>> coordinators;
	private final IOBuilder<TI, TO> ioFactory;

	//final JAXBContext jaxbContext;

	public TaskDefinition(QName name, IOBuilder<TI, TO> ioFactory) {
		this.name = name;
		this.coordinators = new HashSet<TaskActionCoordinatorDefinition<TI, TO>>();
		this.ioFactory = ioFactory;

	}

	public TaskDefinition(QName name, IOBuilder<TI, TO> ioFactory, TaskActionCoordinatorDefinition<TI, TO> coordinatorDefinition) {
		this(name, ioFactory);
		if (coordinatorDefinition != null) {
			this.coordinators.add(coordinatorDefinition);
		}
	}

	public QName task() {
		return name;
	}

	public IOBuilder<TI, TO> ioFactory() {
		return ioFactory;

	}

	public JAXBContext jaxbContext() throws JAXBException {
		Set<Class<?>> ofClasses = new HashSet<Class<?>>();
		for (TaskActionCoordinatorDefinition def : coordinators) {
			for (Class<?> clazz : def.jaxbFactoryClasses) {
				ofClasses.add(clazz);
			}

		}
		return JAXBContext.newInstance(ofClasses.toArray(new Class<?>[ofClasses.size()]));
	}

	public void addTaskActionCoordinator(TaskActionCoordinatorDefinition<TI, TO> def) {
		synchronized (coordinators) {
			coordinators.add(def);
		}
	}

	public Set<TaskActionCoordinatorDefinition<TI, TO>> coordinators() {
		return coordinators;
	}

	public static class TaskActionCoordinatorDefinition<TI, TO> {
		public final QName name;
		public final Set<QName> dependencies;
		public final Class<?>[] jaxbFactoryClasses;
		public final Provider<? extends TaskActionCoordinator<TI, TO>> coordinator;

		public TaskActionCoordinatorDefinition(QName name, Set<QName> dependencies, Provider<? extends TaskActionCoordinator<TI, TO>> coordinator,
				Class<?>... jaxbFactoryClasses) {
			this.name = name;
			this.dependencies = dependencies;
			this.jaxbFactoryClasses = jaxbFactoryClasses;
			this.coordinator = coordinator;
		}
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

	public static String[] targetsToNodeIds(Target[] targets) {
		List<String> nodeIds = new ArrayList<String>();
		for (Target t : targets) {
			for (String nodeId : t.nodeIds()) {
				nodeIds.add(nodeId);
			}
		}
		return nodeIds.toArray(new String[nodeIds.size()]);
	}
}