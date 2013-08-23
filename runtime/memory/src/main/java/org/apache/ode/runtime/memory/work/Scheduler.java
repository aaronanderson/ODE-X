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
package org.apache.ode.runtime.memory.work;

import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.ode.runtime.memory.work.WorkManager.WorkThreadPoolExecutor;
import org.apache.ode.runtime.memory.work.xml.WorkScheduler;
import org.apache.ode.spi.work.ExecutionUnit.ExecutionState;
import org.apache.ode.spi.work.ExecutionUnit.ExecutionUnitState;

public class Scheduler implements Runnable {

	public static Logger log = Logger.getLogger(Scheduler.class.getName());

	volatile boolean running = true;

	WorkThreadPoolExecutor wtp;
	PriorityBlockingQueue<WorkImpl> workQueue;
	long scanTime;

	public Scheduler(WorkScheduler config, WorkThreadPoolExecutor wtp) {
		scanTime = config.getScan() > 0 ? config.getScan() : 500;
		workQueue = new PriorityBlockingQueue<>(config.getQueueSize(), new ExecutionUnitBaseComparator());
		this.wtp = wtp;
	}

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
						Thread.currentThread().sleep(sleep);
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

		for (Iterator<ExecutionStage> i = work.executionQueue.iterator(); i.hasNext();) {
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
				work.hasCancels.compareAndSet(false, true);
				break;

			case CANCEL:
				i.remove();
				work.hasCancels.compareAndSet(false, true);
				break;
			case READY:
			case BLOCK_IN:
				if (ex.inPipesReady()) {
					if (ex.execState.compareAndSet(current, ExecutionState.RUN)) {
						wtp.execute(ex);
					}
				}
				break;
			case BLOCK_SEQ:
				if (ex.checkInDependency(ex.seqDependency)) {
					wtp.execute(ex);
				}
				break;
			case BLOCK_RUN:
				if (ex.frame.block.tryAcquire()) {
					ex.frame.block.release();
					wtp.execute(ex);
				}
				break;
				
			case RUN:
				break;
			case BLOCK_OUT:
				if (ex.outPipesFinished()) {
					wtp.execute(ex);
				}
			}
			break;
		}

		if (work.executionQueue.isEmpty() && work.execState.get() != ExecutionUnitState.RUN) {
			w.remove();
		}

	}

	public void schedule(WorkImpl eu) throws SchedulerException {
		if (!eu.stateChange(ExecutionUnitState.BUILD, ExecutionUnitState.READY)) {
			throw new SchedulerException("Unexpected State");
		}
		if (!workQueue.add(eu)) {
			throw new SchedulerException("Unable to schedule ExecutionUnit");
		} else {

		}
	}

	public boolean cancel(WorkImpl eu) throws SchedulerException {
		if (!eu.stateChange(eu.execState.get(), ExecutionUnitState.CANCEL)) {
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
