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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.ode.runtime.memory.work.xml.WorkConfig;
import org.apache.ode.spi.runtime.Node.NodeStatus;
import org.apache.ode.spi.runtime.Node.Offline;
import org.apache.ode.spi.runtime.Node.Start;
import org.apache.ode.spi.runtime.PlatformException;
import org.apache.ode.spi.work.ExecutionUnit.Work;

@Singleton
@NodeStatus
public class WorkManager {

	private static final Logger log = Logger.getLogger(WorkManager.class.getName());

	@Inject
	WorkConfig workConfig;

	WorkThreadPoolExecutor wtp;

	Scheduler scheduler;

	public Scheduler scheduler() throws PlatformException {
		if (scheduler == null) {
			throw new PlatformException("Scheduler unavailable, perhaps node offline ");
		}
		return scheduler;
	}

	@Start
	public void start() throws PlatformException {
		//try {
		BlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(workConfig.getWorkExec().getQueueSize());
		if (workConfig.getWorkExec().getMaxPool() <= 1) {
			log.warning(String.format("MaxPool Size must be greater than one since scheduler and work must be executed in same pool"));
			workConfig.getWorkExec().setMaxPool(2);
		}
		wtp = new WorkThreadPoolExecutor(workConfig.getWorkExec().getCorePool(), workConfig.getWorkExec().getMaxPool(), workConfig.getWorkExec().getKeepAlive(), TimeUnit.SECONDS,
				queue, new ODEWorkThreadFactory());
		scheduler = new Scheduler(workConfig.getWorkScheduler(), wtp);
		wtp.setRejectedExecutionHandler(scheduler.new SchedulerRejectedExecHandler());
		wtp.execute(scheduler);

		//} catch (Exception e) {
		//	throw new PlatformException(e);
		//	}

	}

	@Offline
	public void offline() throws PlatformException {
		try {
			scheduler.running = false;
			wtp.shutdown();
			wtp.awaitTermination(workConfig.getWorkExec().getShutdownTimeout(), TimeUnit.SECONDS);
			if (!wtp.isTerminated()) {
				wtp.shutdownNow();
			}
		} catch (Exception e) {
			throw new PlatformException(e);
		} finally {
			wtp = null;
			scheduler = null;
		}

	}

	@Singleton
	public static class WorkProvider implements Provider<Work> {

		@Inject
		WorkManager wm;

		@Override
		public Work get() {

			try {
				return new WorkImpl(wm.scheduler());
			} catch (PlatformException e) {
				log.log(Level.SEVERE, "", e);
				return null;
			}
		}

	}

	public static class WorkThreadPoolExecutor extends ThreadPoolExecutor {

		public WorkThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
			super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
		}

	}

	public static class ODEWorkThreadFactory implements ThreadFactory {

		private final ThreadFactory factory = java.util.concurrent.Executors.defaultThreadFactory();
		private long id = 0;

		@Override
		public Thread newThread(Runnable r) {
			Thread t = factory.newThread(r);
			t.setName("ODE-X Work Executor - " + ++id);
			t.setDaemon(true);
			return t;
		}
	}

}
