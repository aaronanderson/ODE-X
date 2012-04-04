package org.apache.ode.runtime.interpreter;

import javax.xml.bind.JAXBElement;

import org.apache.ode.spi.exec.instruction.ExecutionContext;
import org.apache.ode.spi.exec.instruction.Operation;
import org.apache.ode.spi.exec.instruction.xml.ExecutionState;
import org.apache.ode.spi.exec.instruction.xml.Input;
import org.apache.ode.spi.exec.instruction.xml.Result;
import org.apache.ode.spi.exec.instruction.xml.Stack;

/*
  This class is intended as a shield so that instructions may only interact with
  the Execution state via pre-defined Operations
 */
public class ExecutionContextImpl implements ExecutionContext {
	final ExecutionState state;

	public ExecutionContextImpl(ExecutionState state) {
		this.state = state;
	}
	
	public void push(JAXBElement<? extends Stack> newStack){
		JAXBElement<? extends Stack> current = state.getStack();
		state.setStack(newStack);
		newStack.getValue().setStack(current);
	}
	
	public void pop(){
		JAXBElement<? extends Stack> current = state.getStack();
		JAXBElement<? extends Stack> newStack = current.getValue().getStack();
		state.setStack(newStack);
	}

	@Override
	public <I extends Input, R extends Result> R execute(Operation<I, R> op, I input) {
		return op.execute(state, input);
	}

}
