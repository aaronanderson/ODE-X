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


