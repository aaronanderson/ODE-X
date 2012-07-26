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
package org.apache.ode.runtime.wsdl;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.ode.runtime.exec.wsdl.xml.ObjectFactory;
import org.apache.ode.spi.exec.PlatformException;
import org.apache.ode.spi.exec.task.TaskActionDefinition;
import org.apache.ode.spi.exec.task.TaskDefinition;

public abstract class WSDLComponent extends org.apache.ode.spi.exec.WSDLComponent {

	@Override
	public QName name() {
		return WSDL_INSTRUCTION_SET;
	}

	@Override
	public List<InstructionSet> instructionSets() {
		List<InstructionSet> iset = new ArrayList<InstructionSet>();
		iset.add(new InstructionSet(WSDL_INSTRUCTION_SET, "org.apache.ode.runtime.exec.wsdl.xml", ObjectFactory.class));
		return iset;
	}

	@Override
	public List<TaskDefinition> tasks() {
		return null;
	}

	@Override
	public List<TaskActionDefinition> actions() {
		return null;
	}

	@Override
	public void online() throws PlatformException {

	}

	@Override
	public void offline() throws PlatformException {

	}

}
