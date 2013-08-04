package org.apache.ode.runtime.memory.operation;

import java.util.concurrent.TimeUnit;

import org.apache.ode.spi.work.ExecutionUnit.Work;

public class WorkImpl extends ExecutionUnitBase implements Work {

	public WorkImpl(Scheduler scheduler) {
		super(scheduler);
	}

	@Override
	public void submit() throws ExecutionUnitException {
		// TODO Auto-generated method stub

	}

	@Override
	public ExecutionState state(long timeout, TimeUnit unit, ExecutionUnitState... expected) throws ExecutionUnitException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void cancel() throws ExecutionUnitException {
		// TODO Auto-generated method stub

	}

}
