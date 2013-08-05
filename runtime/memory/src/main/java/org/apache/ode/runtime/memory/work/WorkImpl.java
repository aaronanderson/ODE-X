package org.apache.ode.runtime.memory.work;

import java.util.concurrent.TimeUnit;

import org.apache.ode.runtime.memory.work.Scheduler.SchedulerException;
import org.apache.ode.spi.work.ExecutionUnit.Work;

public class WorkImpl extends ExecutionUnitBase implements Work {

	public WorkImpl(ExecutionUnitBase parent, Scheduler scheduler) {
		super(parent, scheduler);
	}

	@Override
	public void submit() throws ExecutionUnitException {
		try {
			scheduler.schedule(this);
		} catch (SchedulerException e) {
			throw new ExecutionUnitException(e);
		}

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
