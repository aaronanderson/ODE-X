package org.apache.ode.runtime.exec.test;

import java.util.Map;

import org.apache.ode.runtime.ectx.test.xml.GetInputTest;
import org.apache.ode.runtime.ectx.test.xml.GetOperationTest;
import org.apache.ode.runtime.ectx.test.xml.GetResultTest;
import org.apache.ode.runtime.ectx.test.xml.TestVariables;
import org.apache.ode.runtime.ectx.test.xml.TestVariables.TestVariable;
import org.apache.ode.spi.exec.InstructionScope;
import org.apache.ode.spi.exec.ListMap;
import org.apache.ode.spi.exec.instruction.Operation;
import org.apache.ode.spi.exec.instruction.xml.ExecutionState;
import org.apache.ode.spi.exec.instruction.xml.Stack;

@InstructionScope
public class GetTestOperation extends GetOperationTest implements Operation<GetInputTest, GetResultTest> {

	@Override
	public GetResultTest execute(ExecutionState state, GetInputTest input) {
		GetResultTest res = new GetResultTest();
		TestVariables  vars = null;
		Stack stack = state.getStack().getValue();
		for (Object o : stack.getAny()) {
			if (o instanceof TestVariables) {
				vars = (TestVariables)o; 
				break;
			}
		}
		if (vars == null) {
			return res;
		}
		TestVariable var = ListMap.get(input.getName(), vars.getVariables());
		if (var !=null){
			res.setValue(var.getValue());
		}
		return res;
	}

}
