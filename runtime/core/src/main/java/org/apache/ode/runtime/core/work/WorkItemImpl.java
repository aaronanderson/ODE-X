package org.apache.ode.runtime.core.work;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;

import org.apache.ode.spi.work.ExecutionUnit.WorkItem;

public class WorkItemImpl extends ExecutionUnitBuilder implements WorkItem {

	ExecutionStage exStage;
	WorkItemInput input;
	WorkItemOutput output;

	public WorkItemImpl(Frame parent, ExecutionStage exStage) {
		super(parent);
		this.exStage = exStage;
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
		if (exStage.block == null) {
			exStage.block = new Semaphore(1);
		}
		return exStage.block;
	}

	@Override
	public void abort(Throwable t) throws ExecutionUnitException {
		// TODO Auto-generated method stub

	}

	@Override
	public <O extends Output> O input() throws ExecutionUnitException {
		if (input == null) {
			input = new WorkItemInput(exStage.input);
		}
		return (O) input;

	}

	@Override
	public <I extends Input> I output() throws ExecutionUnitException {
		if (output == null) {
			output = new WorkItemOutput(exStage);
		}
		return (I) output;
	}

	public static class WorkItemInput extends Stage implements Output {

		public WorkItemInput(Object[] output) {
			super(null, output);
		}

	}

	public static class WorkItemOutput extends Stage implements Input {

		public WorkItemOutput(ExecutionStage execStage) {
			this(execStage, new Object[execStage.output.length]);
		}

		public WorkItemOutput(ExecutionStage execStage, Object[] io) {
			super(io, io);
			if (execStage.outPipes != null) {
				for (Pipe p : execStage.outPipes) {
					if (p.from != null) {
						p.from = this;
					} else {
						Set current = null;
						Set newSet = null;
						do {
							current = (Set) p.froms.get();
							newSet = new HashSet<>(current);
							newSet.remove(execStage);
							newSet.add(this);
						} while (!p.froms.compareAndSet(current, newSet));

					}
				}
			}

		}

		//public static 

	}

}
