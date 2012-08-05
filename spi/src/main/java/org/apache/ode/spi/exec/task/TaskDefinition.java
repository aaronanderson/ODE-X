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

import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;

import org.apache.ode.spi.exec.target.Target;

public class TaskDefinition<TI, TO> {

	private final QName name;
	private final Set<TaskActionCoordinator<TI, ?, ?, TO>> coordinators;
	final JAXBContext jaxbContext;

	public TaskDefinition(QName name, TaskActionCoordinator<TI, ?, ?, TO> coordinator, JAXBContext jaxbContext) {
		this.name = name;
		this.coordinators = new HashSet<TaskActionCoordinator<TI, ?, ?, TO>>();
		coordinators.add(coordinator);
		this.jaxbContext = jaxbContext;
	}

	public QName task() {
		return name;
	}

	public JAXBContext jaxbContext() {
		return jaxbContext;
	}

	public void addTaskActionCoordinator(TaskActionCoordinator<TI, ?, ?, TO> coordinator) {
		synchronized (coordinators) {
			coordinators.add(coordinator);
		}
	}

	public Set<TaskActionCoordinator<TI, ?, ?, TO>> coordinators() {
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