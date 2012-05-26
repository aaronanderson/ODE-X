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
package org.apache.ode.runtime.exec.platform;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.apache.ode.spi.exec.Executors;
import org.apache.ode.spi.exec.PlatformException;

@Singleton
public abstract class ExecutorsImpl implements Executors {

	ScheduledExecutorService taskScheduler;
	private ThreadPoolExecutor taskExec;
	ScheduledExecutorService puScheduler;
	private ThreadPoolExecutor puExec;

	@Override
	public ScheduledExecutorService initClusterTaskScheduler() throws PlatformException {
		taskScheduler = new ScheduledThreadPoolExecutor(2, new ThreadFactory() {

			private final ThreadFactory factory = java.util.concurrent.Executors.defaultThreadFactory();

			@Override
			public Thread newThread(Runnable r) {
				Thread t = factory.newThread(r);
				t.setName("ODE-X Cluster Action Scheduler");
				t.setDaemon(true);
				return t;
			}
		});
		return taskScheduler;

	}

	@Override
	public void destroyClusterTaskScheduler() throws PlatformException {
		taskScheduler.shutdownNow();

	}

	@Override
	public ExecutorService initClusterTaskExecutor(RejectedExecutionHandler handler) throws PlatformException {

		BlockingQueue<Runnable> actionQueue = new ArrayBlockingQueue<Runnable>(getActionQueueSize());

		taskExec = new ThreadPoolExecutor(getActionMinThreads(), getActionMaxThreads(), getActionThreadTimeout(), TimeUnit.SECONDS, actionQueue,
				new ThreadFactory() {

					private final ThreadFactory factory = java.util.concurrent.Executors.defaultThreadFactory();
					private long id = 0;

					@Override
					public Thread newThread(Runnable r) {
						Thread t = factory.newThread(r);
						t.setName("ODE-X Cluster Action Executor - " + ++id);
						t.setDaemon(true);
						return t;
					}
				}, handler);
		taskExec.allowCoreThreadTimeOut(true);
		return taskExec;

	}

	@Override
	public void destroyClusterTaskExecutor() throws PlatformException {
		if (taskExec != null) {
			taskExec.shutdown();
			try {
				taskExec.awaitTermination(getActionShutdownWaitTime(), TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				throw new PlatformException(e);
			}
		}
	}

	@Override
	public ExecutorService initProcessingUnitExecutor(RejectedExecutionHandler handler) throws PlatformException {
		BlockingQueue<Runnable> puQueue = new ArrayBlockingQueue<Runnable>(getPUQueueSize());

		puExec = new ThreadPoolExecutor(getPUMinThreads(), getPUMaxThreads(), getPUThreadTimeout(), TimeUnit.SECONDS, puQueue, new ThreadFactory() {

			private final ThreadFactory factory = java.util.concurrent.Executors.defaultThreadFactory();
			private long id = 0;

			@Override
			public Thread newThread(Runnable r) {
				Thread t = factory.newThread(r);
				t.setName("ODE-X Cluster PU Executor - " + ++id);
				t.setDaemon(true);
				return t;
			}
		}, handler);
		puExec.allowCoreThreadTimeOut(true);
		return puExec;

	}

	@Override
	public void destroyProcessingUnitExecutor() throws PlatformException {
		if (puExec != null) {
			puExec.shutdown();
			try {
				puExec.awaitTermination(getPUShutdownWaitTime(), TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				throw new PlatformException(e);
			}
		}

	}

	@Override
	public ScheduledExecutorService initProcessingUnitScheduler() throws PlatformException {
		puScheduler = new ScheduledThreadPoolExecutor(getPUSchedulerThreads(), new ThreadFactory() {

			private final ThreadFactory factory = java.util.concurrent.Executors.defaultThreadFactory();

			@Override
			public Thread newThread(Runnable r) {
				Thread t = factory.newThread(r);
				t.setName("ODE-X Cluster PU Scheduler");
				t.setDaemon(true);
				return t;
			}
		});
		return puScheduler;

	}

	@Override
	public void destroyProcessingUnitScheduler() throws PlatformException {
		puScheduler.shutdownNow();
	}

	public abstract int getActionQueueSize();

	public abstract int getActionMinThreads();

	public abstract int getActionMaxThreads();

	public abstract long getActionThreadTimeout();

	public abstract long getActionShutdownWaitTime();

	public abstract int getPUSchedulerThreads();

	public abstract int getPUQueueSize();

	public abstract int getPUMinThreads();

	public abstract int getPUMaxThreads();

	public abstract long getPUThreadTimeout();

	public abstract long getPUShutdownWaitTime();

}
