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

import org.apache.ode.runtime.exec.ectx.test.xml.SetInputTest;
import org.apache.ode.runtime.exec.ectx.test.xml.SetOperationTest;
import org.apache.ode.runtime.exec.ectx.test.xml.SetResultTest;
import org.apache.ode.runtime.exec.ectx.test.xml.TestVariables;
import org.apache.ode.runtime.exec.ectx.test.xml.TestVariables.TestVariable;
import org.apache.ode.spi.exec.InstructionScope;
import org.apache.ode.spi.exec.ListMap;
import org.apache.ode.spi.exec.instruction.Operation;
import org.apache.ode.spi.exec.instruction.xml.BlockStack;
import org.apache.ode.spi.exec.instruction.xml.ExecutionState;

@InstructionScope
public class SetTestOperation extends SetOperationTest implements Operation<SetInputTest, SetResultTest> {

	@Override
	public SetResultTest execute(ExecutionState state, SetInputTest input) {
		BlockStack stack = state.getBlock();
		TestVariables vars = null;
		for (Object o : stack.getAny()) {
			if (o instanceof TestVariables) {
				vars = (TestVariables) o;
				break;
			}
		}
		if (vars == null) {
			vars = new TestVariables();
			stack.getAny().add(vars);
		}
		TestVariable var = ListMap.get(input.getName(), vars.getVariables());
		if (var != null) {
			var.setValue(input.getValue());	
		}else{
			var = new TestVariable();
			var.setName(input.getName());
			var.setValue(input.getValue());
			vars.getVariables().add(var);
		}
		SetResultTest res = new SetResultTest();
		res.setStatus("OK");
		return res;
	}

}
