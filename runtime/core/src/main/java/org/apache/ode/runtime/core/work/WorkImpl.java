package org.apache.ode.runtime.core.work;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.xml.namespace.QName;

import org.apache.ode.runtime.core.work.ExecutionUnitBuilder.Frame;
import org.apache.ode.runtime.core.work.WorkScheduler.SchedulerException;
import org.apache.ode.spi.di.DIContainer;
import org.apache.ode.spi.di.OperationAnnotationScanner.OperationModel;
import org.apache.ode.spi.work.ExecutionUnit.Work;

public class WorkImpl extends ExecutionUnitBuilder<Frame> implements Work {
	WorkContext workCtx;

	public WorkImpl(WorkScheduler scheduler, Map<QName, OperationModel> operations, DIContainer dic) {
		super(new Frame(new WorkContext(scheduler, dic, operations)));
		this.workCtx = frame.workCtx;
	}

	@Override
	public void submit() throws ExecutionUnitException {

		workCtx.executionCount.addAndGet(executionBuildQueue.size());
		workCtx.executionQueue.addAll(executionBuildQueue);
		executionBuildQueue.clear();
		try {
			workCtx.scheduler.schedule(this);
		} catch (SchedulerException e) {
			throw new ExecutionUnitException(e);
		}

	}

	@Override
	public ExecutionUnitState state(long timeout, TimeUnit unit, ExecutionUnitState... expected) throws ExecutionUnitException {
		workCtx.stateLock.lock();
		try {
			if (workCtx.changeState.await(timeout, unit)) {
				ExecutionUnitState newState = workCtx.execState.get();
				if (expected != null) {
					for (ExecutionUnitState s : expected) {
						if (s == newState) {
							return newState;
						}
					}
					throw new ExecutionUnitException(String.format("Unexpected state %s", newState));
				} else {
					return newState;
				}
			} else {
				return null;
			}

		} catch (InterruptedException ie) {
			throw new ExecutionUnitException(ie);
		} finally {
			workCtx.stateLock.unlock();
		}
	}

	@Override
	public void cancel() throws ExecutionUnitException {
		try {
			workCtx.scheduler.cancel(this);
		} catch (SchedulerException e) {
			throw new ExecutionUnitException(e);
		}
	}

}
