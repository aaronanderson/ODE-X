package org.apache.ode.runtime.exec.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.ode.runtime.ectx.test.xml.GetInputTest;
import org.apache.ode.runtime.ectx.test.xml.GetResultTest;
import org.apache.ode.runtime.ectx.test.xml.SetInputTest;
import org.apache.ode.runtime.ectx.test.xml.SetResultTest;
import org.apache.ode.runtime.exec.test.xml.InstructionTest;
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
