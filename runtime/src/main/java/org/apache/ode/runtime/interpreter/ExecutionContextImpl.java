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
package org.apache.ode.runtime.interpreter;

import org.apache.ode.spi.exec.instruction.ExecutionContext;
import org.apache.ode.spi.exec.instruction.Operation;
import org.apache.ode.spi.exec.instruction.xml.BlockStack;
import org.apache.ode.spi.exec.instruction.xml.ContextStack;
import org.apache.ode.spi.exec.instruction.xml.ExecutionState;
import org.apache.ode.spi.exec.instruction.xml.Input;
import org.apache.ode.spi.exec.instruction.xml.Result;

/*
  This class is intended as a shield so that instructions may only interact with
  the Execution state via pre-defined Operations
 */
public class ExecutionContextImpl implements ExecutionContext {
	final ExecutionState state;

	public ExecutionContextImpl(ExecutionState state) {
		this.state = state;
	}
	
	public void pushBlock(BlockStack newStack){
		BlockStack current = state.getBlock();
		state.setBlock(newStack);
		newStack.setBlock(current);
	}
	
	public void popBlock(){
		BlockStack current = state.getBlock();
		BlockStack newStack = current.getBlock();
		state.setBlock(newStack);
	}

	public void pushContext(ContextStack newStack){
		BlockStack current = state.getBlock();
		ContextStack currentCtx = current.getContext();
		current.setContext(newStack);
		newStack.setContext(currentCtx);
	}
	
	public void popContext(){
		BlockStack current = state.getBlock();
		ContextStack currentCtx = current.getContext();
		ContextStack newStack = currentCtx.getContext();
		current.setContext(newStack);
	}

	@Override
	public <I extends Input, R extends Result> R execute(Operation<I, R> op, I input) {
		return op.execute(state, input);
	}

}
