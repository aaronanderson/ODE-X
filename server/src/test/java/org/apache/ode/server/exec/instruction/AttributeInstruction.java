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

import org.apache.ode.server.test.xml.AttributeTest;
import org.apache.ode.spi.exec.ExecutableScope;
import org.apache.ode.spi.exec.instruction.Instruction;
import org.apache.ode.spi.exec.instruction.Instruction.ExecutionContext;

@ExecutableScope
public class AttributeInstruction extends AttributeTest implements Instruction<ExecutionContext>{

	@Override
	public Return execute(ExecutionContext execCtx) {
		String test = arg1;
		return Success.success();
		
	}
	
}

