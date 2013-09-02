/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ode.runtime.core.work;

import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.ode.spi.work.ExecutionUnit.ExecutionState;
import org.apache.ode.spi.work.ExecutionUnit.ExecutionUnitState;

public abstract class WorkScheduler implements Runnable {

	public static Logger log = Logger.getLogger(WorkScheduler.class.getName());

	final protected ReentrantLock schedulerLock = new ReentrantLock();
	final protected Condition execReady = schedulerLock.newCondition();

	protected volatile boolean running = true;

	protected ThreadPoolExecutor wtp;
	protected PriorityBlockingQueue<WorkImpl> workQueue;
	protected long scanTime;

	@Override
	public void run() {
		String originalName = Thread.currentThread().getName();
		try {
			Thread.currentThread().setName("ODE-X Work Scheduler");
			while (running) {
				try {
					long start = System.currentTimeMillis();
					for (Iterator<WorkImpl> i = workQueue.iterator(); i.hasNext();) {
						WorkImpl wi = i.next();
						scan(wi, i);
					}
					long sleep = scanTime - (System.currentTimeMillis() - start);
					if (sleep > 0) {
						schedulerLock.lock();
						try {
							execReady.await(sleep, TimeUnit.MILLISECONDS);
						} finally {
							schedulerLock.unlock();
						}
					}
				} catch (SchedulerException ie) {
					log.log(Level.SEVERE, "", ie);
				} catch (InterruptedException ie) {
					running = false;
				}
			}
		} finally {
			Thread.currentThread().setName(originalName);
		}
	}

	/*private void scan(WorkImpl work) throws SchedulerException {
		
	}

	private void scan(WorkItemImpl work) throws SchedulerException {

	}

	private void scan(SequenceImpl work) throws SchedulerException {

	}

	private void scan(ParallelImpl work) throws SchedulerException {

	}*/

	private void scan(WorkImpl work, Iterator<WorkImpl> w) throws SchedulerException {
		//throw new SchedulerException(String.format("unexpected ExecutionUnitBase class %s ", work.getClass()));
		Iterator<ExecutionStage> i = work.workCtx.executionQueue.iterator();
		while (i.hasNext()) {
			ExecutionStage ex = i.next();
			//if (ex.active.get()) { //already executing in threadpool
			//	continue;
			//	}
			ExecutionState current = ex.execState.get();
			switch (current) {
			case COMPLETE:
				i.remove();
				break;

			case ABORT:
				i.remove();
				work.workCtx.hasCancels.compareAndSet(false, true);
				break;

			case CANCEL:
				i.remove();
				work.workCtx.hasCancels.compareAndSet(false, true);
				break;
			case READY:
			case BLOCK_IN:
				if (ex.inPipesReady()) {
					if (!ex.superiorDependencyReady()) {
						ex.execState.compareAndSet(current, ExecutionState.BLOCK_SEQ);
					} else if (ex.execState.compareAndSet(current, ExecutionState.RUN)) {
						wtp.execute(ex);
					}
				}
				break;
			case BLOCK_SEQ:
				if (ex.superiorDependencyReady()) {
					wtp.execute(ex);
				}
				break;
			case BLOCK_RUN:
				if (ex.block.tryAcquire()) {
					ex.block.release();
					wtp.execute(ex);
				}
				break;

			case RUN:
				break;
			case BLOCK_OUT:
				if (ex.outPipesFinished()) {
					wtp.execute(ex);
				}
				break;
			}

		}

		if (work.workCtx.executionQueue.isEmpty() && work.workCtx.execState.get() != ExecutionUnitState.RUN) {
			w.remove();
		}

	}

	public void stop() {
		running = false;
	}

	public void schedule(WorkImpl eu) throws SchedulerException {
		if (!eu.workCtx.stateChange(ExecutionUnitState.BUILD, ExecutionUnitState.READY)) {
			throw new SchedulerException("Unexpected State");
		}
		if (!workQueue.add(eu)) {
			throw new SchedulerException("Unable to schedule ExecutionUnit");
		}
		signalScheduler();
	}

	public void signalScheduler() {
		schedulerLock.lock();
		try {
			execReady.signal();
		} finally {
			schedulerLock.unlock();
		}
	}

	public boolean cancel(WorkImpl eu) throws SchedulerException {
		if (!eu.workCtx.stateChange(eu.workCtx.execState.get(), ExecutionUnitState.CANCEL)) {
			return false;
		}
		return true;
	}

	public class SchedulerRejectedExecHandler implements RejectedExecutionHandler {

		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
			log.warning(String.format("Rejected execution %s", (ExecutionStage) r));

		}

	}

	public static class ExecutionUnitBaseComparator implements Comparator<ExecutionUnitBuilder> {

		@Override
		public int compare(ExecutionUnitBuilder e1, ExecutionUnitBuilder e2) {
			// TODO Auto-generated method stub
			return 0;
		}

	}

	public static class SchedulerException extends Exception {
		public SchedulerException(String msg) {
			super(msg);
		}

		public SchedulerException(Throwable e) {
			super(e);
		}
	}

}
