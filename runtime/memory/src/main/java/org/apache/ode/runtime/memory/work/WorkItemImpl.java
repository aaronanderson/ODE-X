package org.apache.ode.runtime.memory.work;

import java.util.concurrent.Semaphore;

import javax.xml.namespace.QName;

import org.apache.ode.runtime.memory.work.Scheduler.SchedulerException;
import org.apache.ode.spi.work.ExecutionUnit.ExecutionUnitException;
import org.apache.ode.spi.work.ExecutionUnit.WorkItem;

public class WorkItemImpl extends ExecutionUnitBase implements WorkItem {

	public WorkItemImpl(ExecutionUnitBase parent) {
		super(parent, parent.scheduler);
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
	public Semaphore block() throws ExecutionUnitException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void abort(Throwable t) throws ExecutionUnitException {
		// TODO Auto-generated method stub

	}

}
