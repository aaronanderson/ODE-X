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
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.ode.runtime.memory.work.WorkManager.WorkThreadPoolExecutor;
import org.apache.ode.runtime.memory.work.xml.WorkScheduler;

public class Scheduler implements Runnable {

	public static Logger log = Logger.getLogger(Scheduler.class.getName());

	volatile boolean running = true;
	
	WorkThreadPoolExecutor wtp;
	PriorityBlockingQueue<? super ExecutionUnitBase> executionUnitQueue;
	long scanTime;

	public Scheduler(WorkScheduler config, WorkThreadPoolExecutor wtp) {
		scanTime = config.getScan() > 0 ? config.getScan() : 500;
		executionUnitQueue = new PriorityBlockingQueue<>(config.getQueueSize(), new ExecutionUnitBaseComparator());
		this.wtp=wtp;
	}

	@Override
	public void run() {
		try {
			Thread.currentThread().setName("ODE-X Work Scheduler");
			while (running) {
				try {
					long start = System.currentTimeMillis();
					ExecutionUnitBase eu = null;
					while ((eu = (ExecutionUnitBase) executionUnitQueue.poll()) != null) {
						scan(eu);
					}
					long sleep = scanTime - (System.currentTimeMillis() - start);
					if (scanTime > 0) {
						Thread.currentThread().sleep(sleep);
					}
				} catch (SchedulerException ie) {
					log.log(Level.SEVERE, "", ie);
				} catch (InterruptedException ie) {
					running = false;
				}
			}
		} finally {
			Thread.currentThread().setName(null);
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

	private void scan(ExecutionUnitBase work) throws SchedulerException {
		//throw new SchedulerException(String.format("unexpected ExecutionUnitBase class %s ", work.getClass()));
		if (work.executionQueue.peek() instanceof InstanceExec) {
			wtp.execute((InstanceExec) work.executionQueue.peek());
		}
	}

	public void schedule(ExecutionUnitBase eu) throws SchedulerException {
		if (!executionUnitQueue.add(eu)) {
			throw new SchedulerException("Unable to schedule ExecutionUnit");
		}
	}

	public class SchedulerRejectedExecHandler implements RejectedExecutionHandler {

		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
			log.warning(String.format("Rejected execution %s", (ExecutionBase) r));

		}

	}

	public static class ExecutionUnitBaseComparator implements Comparator<ExecutionUnitBase> {

		@Override
		public int compare(ExecutionUnitBase e1, ExecutionUnitBase e2) {
			// TODO Auto-generated method stub
			return 0;
		}

	}

	public static class SchedulerException extends Exception {
		public SchedulerException(String msg) {
			super(msg);
		}
	}

}
