package org.apache.ode.runtime.memory.work;

import org.apache.ode.runtime.memory.work.ExecutionUnitBase.ExecutionType;
import org.apache.ode.spi.work.ExecutionUnit.ParallelExecutionUnit;

public class ParallelImpl extends ExecutionUnitBase implements ParallelExecutionUnit, ExecutionType {

	public ParallelImpl(ExecutionUnitBase parent, Scheduler scheduler) {
		super(parent, scheduler);
	}

}
