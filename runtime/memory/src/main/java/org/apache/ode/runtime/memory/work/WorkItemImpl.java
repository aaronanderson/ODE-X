package org.apache.ode.runtime.memory.work;

import java.util.concurrent.Semaphore;

import org.apache.ode.runtime.memory.work.ExecutionUnitBuilder.Frame;
import org.apache.ode.runtime.memory.work.WorkImpl.RootFrame;
import org.apache.ode.spi.work.ExecutionUnit.WorkItem;

public class WorkItemImpl extends ExecutionUnitBuilder<Frame> implements WorkItem {

	WorkImpl work;

	public WorkItemImpl(Frame parent) {
		super(parent);
		Frame f = frame;
		while (f != null) {
			if (f instanceof RootFrame) {
				this.work = ((RootFrame) f).work;
			}
			f = f.parentFrame;
		}

	}

	@Override
	public void submit() throws ExecutionUnitException {
		if (work == null) {
			throw new ExecutionUnitException("RootFrame not found");
		}
		work.executionCount.addAndGet(executionBuildQueue.size());
		work.executionQueue.addAll(executionBuildQueue);
		executionBuildQueue.clear();

	}

	@Override
	public Semaphore block() throws ExecutionUnitException {
		if (frame.block == null) {
			frame.block = new Semaphore(1);
		}
		return frame.block;
	}

	@Override
	public void abort(Throwable t) throws ExecutionUnitException {
		// TODO Auto-generated method stub

	}

	@Override
	public <I extends InBuffer> I inBuffer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <O extends OutBuffer> O outBuffer() {
		// TODO Auto-generated method stub
		return null;
	}

}
