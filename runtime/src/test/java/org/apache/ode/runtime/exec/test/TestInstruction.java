package org.apache.ode.runtime.exec.test;

import javax.inject.Inject;

import org.apache.ode.runtime.exec.test.xml.InstructionTest;
import org.apache.ode.spi.exec.instruction.ExecutionContext;
import org.apache.ode.spi.exec.instruction.Instruction;

public class TestInstruction extends InstructionTest implements Instruction{

	@Inject
	TestCtxObjectFactory of;
	
	@Override
	public void execute(ExecutionContext execCtx) {
		
		
	}
	
}


