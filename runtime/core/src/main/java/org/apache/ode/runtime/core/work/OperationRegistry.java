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
package org.apache.ode.runtime.core.work;

import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.namespace.QName;

import org.apache.ode.spi.di.CommandAnnotationScanner.CommandModel;
import org.apache.ode.spi.di.CommandAnnotationScanner.Commands;
import org.apache.ode.spi.di.DispatchAnnotationScanner.DispatcherModel;
import org.apache.ode.spi.di.DispatchAnnotationScanner.Dispatches;
import org.apache.ode.spi.di.OperationAnnotationScanner.OperationModel;
import org.apache.ode.spi.di.OperationAnnotationScanner.Operations;
import org.apache.ode.spi.work.ExecutionUnit.Execution;
import org.apache.ode.spi.work.ExecutionUnit.ExecutionUnitException;

@Singleton
public class OperationRegistry {

	private static final Logger log = Logger.getLogger(OperationRegistry.class.getName());

	@Inject
	@Operations
	Map<QName, OperationModel> operations;

	@Inject
	@Commands
	Map<QName, CommandModel> commands;

	@Inject
	@Dispatches
	Set<DispatcherModel> dispatchers;

	@PostConstruct
	public void init() {

	}

	public OperationModel resolveOperation(QName operationName) throws ExecutionUnitException {
		OperationModel model = operations.get(operationName);
		if (model == null) {
			throw new ExecutionUnitException(String.format("Unknown operation %s", operationName));
		}
		return model;
	}

	public <E extends Execution> E resolveBuildCommand(QName commandName, ExecutionUnitBuilder eu) throws ExecutionUnitException {
		CommandModel model = commands.get(commandName);
		if (model == null) {
			throw new ExecutionUnitException(String.format("Unknown command %s", commandName));
		}
		CommandExec ce = new CommandExec(eu.frame, model);
		eu.executionBuildQueue.offer(ce);
		return (E) ce;
	}

	public OperationModel resolveExecCommand(CommandModel model, Object[] input) throws ExecutionUnitException {
		//if (newModel == null) {
		//throw new ExecutionUnitException(String.format("Unable to resolve command execution %s", cm.commandName()));
		//	}
		return null;
	}

}
