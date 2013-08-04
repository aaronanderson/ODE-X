package org.apache.ode.runtime.memory.operation;

import java.util.concurrent.Semaphore;

import javax.xml.namespace.QName;

import org.apache.ode.spi.work.ExecutionUnit.WorkItem;

public  class WorkItemImpl extends ExecutionUnitBase implements WorkItem {

	public WorkItemImpl(Scheduler scheduler) {
		super(scheduler);
	}

	@Override
	public void submit() throws ExecutionUnitException {
		// TODO Auto-generated method stub
		
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
