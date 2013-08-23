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
	final protected Map<ExecutionUnitState, Set<Condition>> specificStateListeners = new HashMap<>();

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

	final public boolean stateChange(ExecutionUnitState currentState, ExecutionUnitState newState) {
		stateLock.lock();
		try {
			if (execState.compareAndSet(currentState, newState)) {
				changeState.signalAll();
				Set<Condition> conds = specificStateListeners.get(newState);
				if (conds != null) {
					for (Condition c : conds) {
						c.signalAll();
					}
				}
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
			if (expected != null) {
				Condition cond = stateLock.newCondition();
				for (ExecutionUnitState state : expected) {
					synchronized (specificStateListeners) {
						Set<Condition> conds = specificStateListeners.get(state);
						if (conds == null) {
							conds = new HashSet<>();
							specificStateListeners.put(state, conds);
						}
						conds.add(cond);
					}

				}
				if (cond.await(timeout, unit)) {
					return execState.get();
				} else {
					return null;
				}
			} else {
				if (changeState.await(timeout, unit)) {
					return execState.get();
				} else {
					return null;
				}

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
