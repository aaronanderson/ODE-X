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

import javax.inject.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;

public class CoordinatedTaskActionDefinition<I, CI, CO, O> extends TaskActionDefinition<I, O> {
	//final Provider<? extends CoordinatedTaskActionExec<I, CI, CO, O>> actionExec;

	public CoordinatedTaskActionDefinition(QName name, Set<QName> dependencies, Provider<? extends CoordinatedTaskActionExec<I, CI, CO, O>> actionExec,
			JAXBContext jaxbContext) {
		super(name, dependencies, actionExec, jaxbContext);
	}

	//@SuppressWarnings("unchecked")
	@Override
	public Provider<? extends CoordinatedTaskActionExec<I, CI, CO, O>> actionExec() {
		return (Provider<? extends CoordinatedTaskActionExec<I, CI, CO, O>>) actionExec;
	}
}