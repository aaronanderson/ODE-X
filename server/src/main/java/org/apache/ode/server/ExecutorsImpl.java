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
package org.apache.ode.server;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.ode.server.xml.ActionExecution;
import org.apache.ode.server.xml.ServerConfig;
import org.apache.ode.spi.exec.Executors;
import org.apache.ode.spi.exec.PlatformException;

@Singleton
public class ExecutorsImpl implements Executors {

	@Inject
	ServerConfig serverConfig;

	ScheduledExecutorService clusterScheduler;
	private ThreadPoolExecutor exec;

	@Override
	public ScheduledExecutorService initClusterActionScheduler() throws PlatformException {
		clusterScheduler = new ScheduledThreadPoolExecutor(2, new ThreadFactory() {

			private final ThreadFactory factory = java.util.concurrent.Executors.defaultThreadFactory();

			@Override
			public Thread newThread(Runnable r) {
				Thread t = factory.newThread(r);
				t.setName("ODE-X Cluster Scheduler");
				t.setDaemon(true);
				return t;
			}
		});
		return clusterScheduler;

	}

	@Override
	public void destroyClusterActionScheduler() throws PlatformException {
		clusterScheduler.shutdownNow();

	}

	@Override
	public ExecutorService initClusterActionExecutor(RejectedExecutionHandler handler) throws PlatformException {

		ActionExecution config = serverConfig.getActionExecution();
		BlockingQueue<Runnable> actionQueue = new ArrayBlockingQueue<Runnable>(config.getQueueSize());

		exec = new ThreadPoolExecutor(config.getMaxThreads(), config.getMaxThreads(), config.getThreadTimeout(), TimeUnit.SECONDS, actionQueue,
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
		exec.allowCoreThreadTimeOut(true);
		return exec;

	}

	@Override
	public void destroyClusterActionExecutor() throws PlatformException {
		if (exec != null) {
			exec.shutdown();
			try {
				exec.awaitTermination(serverConfig.getActionExecution().getShutdownWaitTime(), TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				throw new PlatformException(e);
			}
		}
	}
	
	
}
