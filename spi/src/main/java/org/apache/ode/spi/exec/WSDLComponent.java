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

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

public abstract class WSDLComponent implements Component {

	public static final String WSDL_INSTRUCTION_SET_NS = "http://ode.apache.org/wsdl";
	public static final QName WSDL_INSTRUCTION_SET = new QName(WSDL_INSTRUCTION_SET_NS, "WSDL");

	@Override
	public List<InstructionSet> instructionSets() {
		List<InstructionSet> instructions = new ArrayList<InstructionSet>();
		InstructionSet is = new InstructionSet(WSDL_INSTRUCTION_SET, null);
		instructions.add(is);
		return instructions;
	}

}
