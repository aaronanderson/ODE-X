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

import java.util.List;

import javax.xml.namespace.QName;

import org.apache.ode.spi.exec.task.TaskActionDefinition;
import org.apache.ode.spi.exec.task.TaskDefinition;

public interface Component {

	public QName name();

	public List<InstructionSet> instructionSets();

	public List<TaskDefinition> tasks();
	
	public List<TaskActionDefinition> actions();

	public void online() throws PlatformException;

	public void offline() throws PlatformException;

	public class InstructionSet {
		final QName name;
		final String jaxbExecPath;
		final Class<?> jaxbExecFactory;
		final String jaxbExecContextPath;
		final Class<?> jaxbExecContextFactory;

		public InstructionSet(QName name, String jaxbExecPath, Class<?> jaxbExecFactory) {
			this.name = name;
			this.jaxbExecPath = jaxbExecPath;
			this.jaxbExecFactory = jaxbExecFactory;
			this.jaxbExecContextPath = null;
			this.jaxbExecContextFactory = null;
		}

		public InstructionSet(QName name, String jaxbExecPath, Class<?> jaxbExecFactory, String jaxbExecContextPath, Class<?> jaxbExecContextFactory) {
			this.name = name;
			this.jaxbExecPath = jaxbExecPath;
			this.jaxbExecFactory = jaxbExecFactory;
			this.jaxbExecContextPath = jaxbExecContextPath;
			this.jaxbExecContextFactory = jaxbExecContextFactory;
		}

		public QName getName() {
			return name;
		}

		public String getJAXBExecPath() {
			return jaxbExecPath;
		}

		public Class<?> getJAXBExecFactory() {
			return jaxbExecFactory;
		}

		public String getJAXBExecContextPath() {
			return jaxbExecContextPath;
		}

		public Class<?> getJAXBExecContextFactory() {
			return jaxbExecContextFactory;
		}
	}

}
