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

public class Definition extends Unit {
	private List<Message> messages = new ArrayList<Message>();
	private List<PortType> portTypes = new ArrayList<PortType>();
	private List<Binding> bindings = new ArrayList<Binding>();
	private List<Service> services = new ArrayList<Service>();
	public static final QName DEFINITIONS = new QName(WSDL_NS, "definitions");

	public Definition() {
		super(DEFINITIONS);
	}

	public List<Message> messages() {
		return messages;
	}

	public List<PortType> portTypes() {
		return portTypes;
	}

	public List<Binding> bindings() {
		return bindings;
	}

	public List<Service> services() {
		return services;
	}

}
