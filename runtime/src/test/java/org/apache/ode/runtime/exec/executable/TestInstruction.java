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
package org.apache.ode.runtime.exec.executable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.ode.runtime.ectx.test.xml.GetInputTest;
import org.apache.ode.runtime.ectx.test.xml.GetResultTest;
import org.apache.ode.runtime.ectx.test.xml.SetInputTest;
import org.apache.ode.runtime.ectx.test.xml.SetResultTest;
import org.apache.ode.runtime.exec.executable.test.xml.InstructionTest;
import org.apache.ode.spi.exec.ExecutableScope;
import org.apache.ode.spi.exec.instruction.ExecutionContext;
import org.apache.ode.spi.exec.instruction.Instruction;

@ExecutableScope
public class TestInstruction extends InstructionTest implements Instruction {

	@Inject
	TestCtxObjectFactory of;

	@Inject
	Provider<SetTestOperation> setOp;

	@Inject
	Provider<GetTestOperation> getOp;

	@Override
	public void execute(ExecutionContext execCtx) {
		assertNotNull(execCtx);
		assertNotNull(setOp);
		assertNotNull(getOp);
		SetInputTest input = new SetInputTest();
		input.setName(this.arg1);
		input.setValue(this.arg2);
		SetResultTest result = execCtx.execute(setOp.get(), input);
		assertNotNull(result);
		assertEquals("OK", result.getStatus());
		GetInputTest input2 = new GetInputTest();
		input2.setName(this.arg1);
		GetResultTest result2 = execCtx.execute(getOp.get(), input2);
		assertNotNull(result2);
		assertEquals(arg2, result2.getValue());

	}

}
