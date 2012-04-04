package org.apache.ode.runtime.exec.test;

import org.apache.ode.runtime.ectx.test.xml.SetInputTest;
import org.apache.ode.runtime.ectx.test.xml.SetOperationTest;
import org.apache.ode.runtime.ectx.test.xml.SetResultTest;
import org.apache.ode.runtime.ectx.test.xml.TestVariables;
import org.apache.ode.runtime.ectx.test.xml.TestVariables.TestVariable;
import org.apache.ode.spi.exec.InstructionScope;
import org.apache.ode.spi.exec.ListMap;
import org.apache.ode.spi.exec.instruction.Operation;
import org.apache.ode.spi.exec.instruction.xml.ExecutionState;
import org.apache.ode.spi.exec.instruction.xml.Stack;

@InstructionScope
public class SetTestOperation extends SetOperationTest implements Operation<SetInputTest, SetResultTest> {

	@Override
	public SetResultTest execute(ExecutionState state, SetInputTest input) {
		Stack stack = state.getStack().getValue();
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
