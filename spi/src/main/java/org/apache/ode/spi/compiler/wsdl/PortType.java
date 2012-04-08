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
package org.apache.ode.spi.compiler.wsdl;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

public class PortType extends Unit {

	public static final QName PORT_TYPE = new QName(WSDL_NS, "portType");
	public static final QName OPERATION = new QName(WSDL_NS, "operation");
	public static final QName PARAM = new QName(WSDL_NS, "param");
	public static final QName FAULT = new QName(WSDL_NS, "fault");
	private List<Operation> messages = new ArrayList<Operation>();

	public PortType() {
		super(PORT_TYPE);
	}

	public List<Operation> operations() {
		return operations();
	}

	public static class Operation extends Unit {
		String opName;
		String parameterOrder;

		private List<Param> inputs = new ArrayList<Param>();
		private List<Param> outputs = new ArrayList<Param>();
		private List<Fault> faults = new ArrayList<Fault>();

		public Operation() {
			super(OPERATION);
		}

		public String opName() {
			return opName;
		}

	
		public List<Param> inputs() {
			return inputs;
		}
		
		public List<Param> outputs() {
			return outputs;
		}

		public List<Fault> faults() {
			return faults;
		}

		public String parameterOrder() {
			return parameterOrder;
		}

	}

	public static class Param extends Unit {
		public Param() {
			super(PARAM);
		}

	}

	public static class Fault extends Unit {
		public Fault() {
			super(FAULT);
		}
	}

}
