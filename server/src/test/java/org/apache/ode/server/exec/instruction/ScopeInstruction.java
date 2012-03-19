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
package org.apache.ode.server.exec.instruction;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.ode.server.test.xml.ScopeTest;
import org.apache.ode.spi.exec.ExecutableScope;
import org.apache.ode.spi.exec.instruction.Instruction;
import org.apache.ode.spi.exec.instruction.Instruction.ExecutionContext;

@ExecutableScope
public class ScopeInstruction extends ScopeTest implements Instruction<ExecutionContext>{

	private boolean started=false;
	private boolean stopped=false;
	
	@Inject
	@Named("shared")
	private ExecutableShared shared=null;
	
	
	@PostConstruct
	public void start(){
		started=true;
	}
	
	@PreDestroy
	public void stop(){
		stopped=true;
	}
	
	@Override
	public Return execute(ExecutionContext execCtx) {
		return Success.success();
		
	}

	public boolean isStarted() {
		return started;
	}

	public boolean isStopped() {
		return stopped;
	}

	public ExecutableShared getShared() {
		return shared;
	}
	
	
	
}


