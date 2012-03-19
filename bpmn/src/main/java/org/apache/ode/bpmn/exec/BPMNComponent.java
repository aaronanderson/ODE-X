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
package org.apache.ode.bpmn.exec;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.ode.spi.exec.Action;
import org.apache.ode.spi.exec.Component;
import org.apache.ode.spi.exec.PlatformException;

public class BPMNComponent implements Component {

	public static final String BPMN_INSTRUCTION_SET_NS = "http://ode.apache.org/bpmn";
	public static final QName BPMN_INSTRUCTION_SET = new QName(BPMN_INSTRUCTION_SET_NS, "BPMN");

	@Override
	public QName name() {
		return BPMN_INSTRUCTION_SET;
	}

	@Override
	public List<InstructionSet> instructionSets() {
		List<InstructionSet> iset = new ArrayList<InstructionSet>();
		iset.add(new InstructionSet(BPMN_INSTRUCTION_SET, "org.apache.ode.bpmn.exec.xml",null));
		return iset;
	}

	@Override
	public List<Action> actions() {
		return null;
	}

	@Override
	public void online() throws PlatformException {
		// TODO Auto-generated method stub

	}

	@Override
	public void offline() throws PlatformException {
		// TODO Auto-generated method stub

	}

}
