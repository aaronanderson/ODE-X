package org.apache.ode.spi.exec.instruction;

import org.apache.ode.spi.exec.instruction.xml.Input;
import org.apache.ode.spi.exec.instruction.xml.Result;

public interface ExecutionContext {

	<I extends Input, R extends Result> R execute(Operation<I, R> op, I input);

}