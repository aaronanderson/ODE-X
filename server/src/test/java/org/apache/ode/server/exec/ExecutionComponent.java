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
package org.apache.ode.server.exec;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.namespace.QName;

import org.apache.ode.server.exec.instruction.TestObjectFactory;
import org.apache.ode.spi.exec.Action;
import org.apache.ode.spi.exec.Component;
import org.apache.ode.spi.exec.Platform;
import org.apache.ode.spi.exec.PlatformException;

@Singleton
public class ExecutionComponent implements Component {
	public static final String TEST_NS = "http://ode.apache.org/server-executable-test";
	public static final QName COMPONENT_NAME = new QName(TEST_NS, "TestComponent");
	List<Action> supportedActions = new ArrayList<Action>();

	
	@Inject
	Platform platform;
	
	private static final Logger log = Logger.getLogger(ExecutionComponent.class.getName());


	@PostConstruct
	public void init() {
		log.fine("Initializing ExecutionComponent");
		platform.registerComponent(this);
		log.fine("ExecutionComponent Initialized");

	}

	@Override
	public QName name() {
		return COMPONENT_NAME;
	}

	@Override
	public List<InstructionSet> instructionSets() {
		List<InstructionSet> instructions = new ArrayList<InstructionSet>();
		instructions.add(new InstructionSet(COMPONENT_NAME, "org.apache.ode.server.test.xml",TestObjectFactory.class));
		return instructions;
	}

	@Override
	public List<Action> actions() {
		return supportedActions;
	}

	
	@Override
	public void online() throws PlatformException {

	}

	@Override
	public void offline() throws PlatformException {

	}

}
