package org.apache.ode.spi.exec.instruction;

import org.apache.ode.spi.exec.instruction.xml.ExecutionState;
public interface Operation<I,R> {
	
	 R execute(ExecutionState state, I input);//, List<JAXBElement<? extends OperatorContext>> operatorContexts);

}