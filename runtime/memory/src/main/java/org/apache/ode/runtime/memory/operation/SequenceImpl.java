package org.apache.ode.runtime.memory.operation;

import org.apache.ode.spi.work.ExecutionUnit.SequentialExecutionUnit;


public class SequenceImpl  extends ExecutionUnitBase implements SequentialExecutionUnit {
	
	
	public SequenceImpl(Scheduler scheduler) {
		super(scheduler);
	}
	
	

}
