package org.apache.ode.runtime.memory.work;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.ode.runtime.memory.work.ExecutionUnitBuilder.Frame;
import org.apache.ode.runtime.memory.work.Scheduler.SchedulerException;
import org.apache.ode.runtime.memory.work.WorkImpl.RootFrame;
import org.apache.ode.spi.work.ExecutionUnit.Work;

public class WorkImpl extends ExecutionUnitBuilder<RootFrame> implements Work {

	protected AtomicReference<ExecutionUnitState> execState = new AtomicReference<>(ExecutionUnitState.BUILD);
	public AtomicBoolean hasCancels = new AtomicBoolean(false);
	final protected AtomicInteger executionCount = new AtomicInteger();
	final protected Queue<ExecutionStage> executionQueue = new ConcurrentLinkedQueue<>();
	final protected ReentrantLock stateLock = new ReentrantLock();
	final protected Condition changeState = stateLock.newCondition();

	protected Scheduler scheduler;

	public static class RootFrame extends Frame {
		protected WorkImpl work;

		public RootFrame() {
			super(null);

		}
	}

	public WorkImpl(Scheduler scheduler) {
		super(new RootFrame());
		frame.work = this;
		this.scheduler = scheduler;
	}

	@Override
	public void submit() throws ExecutionUnitException {

		executionCount.addAndGet(executionBuildQueue.size());
		executionQueue.addAll(executionBuildQueue);
		executionBuildQueue.clear();
		try {
			scheduler.schedule(this);
		} catch (SchedulerException e) {
			throw new ExecutionUnitException(e);
		}

	}

	public void signalExecWaiting() throws ExecutionUnitException {
		scheduler.signalScheduler();
	}

	final public boolean stateChange(ExecutionUnitState currentState, ExecutionUnitState newState) {
		stateLock.lock();
		try {
			if (execState.compareAndSet(currentState, newState)) {
				changeState.signalAll();
				return true;
			}
			return false;
		} finally {
			stateLock.unlock();
		}
	}

	@Override
	public ExecutionUnitState state(long timeout, TimeUnit unit, ExecutionUnitState... expected) throws ExecutionUnitException {
		stateLock.lock();
		try {
			if (changeState.await(timeout, unit)) {
				ExecutionUnitState newState = execState.get();
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
			stateLock.unlock();
		}
	}

	@Override
	public void cancel() throws ExecutionUnitException {
		try {
			scheduler.cancel(this);
		} catch (SchedulerException e) {
			throw new ExecutionUnitException(e);
		}
	}

}
