package org.apache.ode.runtime.memory.work;

import org.apache.ode.runtime.memory.work.ExecutionUnitBase.ExecutionType;
import org.apache.ode.spi.work.ExecutionUnit.SequentialExecutionUnit;

public class SequenceImpl extends ExecutionUnitBase implements SequentialExecutionUnit, ExecutionType {

	public SequenceImpl(ExecutionUnitBase parent, Scheduler scheduler) {
		super(parent, scheduler);
	}

}
