package org.apache.ode.runtime.core.work;

import java.util.concurrent.Semaphore;

import org.apache.ode.runtime.core.work.ExecutionUnitBuilder.Frame;
import org.apache.ode.spi.work.ExecutionUnit.WorkItem;

public class WorkItemImpl extends ExecutionUnitBuilder<Frame> implements WorkItem {

	final ThreadLocal<ExecutionStage> currentExecution = new ThreadLocal<>();

	public WorkItemImpl(Frame parent) {
		super(parent);
	}

	@Override
	public void submit() throws ExecutionUnitException {
		if (frame.workCtx == null) {
			throw new ExecutionUnitException("WorkCtx not found");
		}
		frame.workCtx.executionCount.addAndGet(executionBuildQueue.size());
		frame.workCtx.executionQueue.addAll(executionBuildQueue);
		executionBuildQueue.clear();

	}

	@Override
	public Semaphore block() throws ExecutionUnitException {
		ExecutionStage current = currentExecution.get();
		if (current == null) {
			throw new ExecutionUnitException("Unable to obtain current Execution");
		}
		if (current.block == null) {
			current.block = new Semaphore(1);
		}
		return current.block;
	}

	@Override
	public void abort(Throwable t) throws ExecutionUnitException {
		// TODO Auto-generated method stub

	}

	@Override
	public <I extends InBuffer> I inBuffer() throws ExecutionUnitException {
		ExecutionStage current = currentExecution.get();
		if (current == null) {
			throw new ExecutionUnitException("Unable to obtain current Execution");
		}
		return null;
	}

	@Override
	public <O extends OutBuffer> O outBuffer() throws ExecutionUnitException {
		ExecutionStage current = currentExecution.get();
		if (current == null) {
			throw new ExecutionUnitException("Unable to obtain current Execution");
		}
		return null;
	}

}
