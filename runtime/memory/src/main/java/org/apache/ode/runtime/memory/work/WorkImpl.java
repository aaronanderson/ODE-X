package org.apache.ode.runtime.memory.work;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.ode.runtime.memory.work.Scheduler.SchedulerException;
import org.apache.ode.spi.work.ExecutionUnit.Work;

public class WorkImpl extends ExecutionUnitBuilder implements Work {

	protected AtomicReference<ExecutionUnitState> execState = new AtomicReference<>(ExecutionUnitState.BUILD);
	public AtomicBoolean hasCancels = new AtomicBoolean(false);
	protected ReentrantLock stateLock = new ReentrantLock();
	protected Condition changeState = stateLock.newCondition();
	protected Map<ExecutionUnitState, Set<Condition>> specificStateListeners = new HashMap<>();

	protected Queue<ExecutionStage> executionQueue;
	protected Scheduler scheduler;

	public WorkImpl(Scheduler scheduler) {
		super();
		this.scheduler = scheduler;
		executionQueue = ((RootFrame) frame).executionQueue;

	}

	@Override
	public void submit() throws ExecutionUnitException {
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
