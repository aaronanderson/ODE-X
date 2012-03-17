package org.apache.ode.runtime.interpreter;

import org.apache.ode.runtime.exec.test.xml.InstructionTest;
import org.apache.ode.spi.exec.instruction.Instruction;
import org.apache.ode.spi.exec.instruction.Instruction.ExecutionContext;

public class TestInstruction extends InstructionTest implements Instruction<ExecutionContext>{

	@Override
	public Return execute(ExecutionContext execCtx) {
		String test = arg1;
		return Success.success();
		
	}
	
}


