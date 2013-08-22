package org.apache.ode.runtime.memory.work;

import java.util.Queue;
import java.util.concurrent.Semaphore;

import org.apache.ode.spi.work.ExecutionUnit.WorkItem;

public class WorkItemImpl extends ExecutionUnitBuilder implements WorkItem {

	Queue<ExecutionStage> executionQueue;

	public WorkItemImpl(Frame parent) {
		super(parent);
		Frame f = frame;
		while (f != null) {
			if (f instanceof RootFrame) {
				this.executionQueue = ((RootFrame) f).executionQueue;
			}
			f = f.parentFrame;
		}

	}

	@Override
	public void submit() throws ExecutionUnitException {
		if (executionQueue == null) {
			throw new ExecutionUnitException("RootFrame not found");
		}
		executionQueue.addAll(executionBuildQueue);
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
	public <I extends Buffer> I inBuffer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <O extends Buffer> O outBuffer() {
		// TODO Auto-generated method stub
		return null;
	}

}
