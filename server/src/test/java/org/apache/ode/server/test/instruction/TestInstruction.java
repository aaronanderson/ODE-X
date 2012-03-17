package org.apache.ode.server.test.instruction;

import org.apache.ode.server.test.xml.InstructionTest;
import org.apache.ode.spi.exec.ExecutableScope;
import org.apache.ode.spi.exec.instruction.Instruction;
import org.apache.ode.spi.exec.instruction.Instruction.ExecutionContext;

@ExecutableScope
public class TestInstruction extends InstructionTest implements Instruction<ExecutionContext>{

	@Override
	public Return execute(ExecutionContext execCtx) {
		String test = arg1;
		return Success.success();
		
	}
	
}


