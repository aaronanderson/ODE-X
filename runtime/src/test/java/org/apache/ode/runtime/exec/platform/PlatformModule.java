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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.ode.runtime.exec.platform.task.TaskExecutor;
import org.apache.ode.runtime.exec.platform.task.TaskPoll;
import org.apache.ode.spi.exec.Executors;
import org.apache.ode.spi.exec.Platform;
import org.apache.ode.spi.exec.PlatformException;

import com.google.inject.AbstractModule;

public class PlatformModule extends AbstractModule {
	protected void configure() {
		bind(HealthCheck.class);
		bind(TaskPoll.class);
		bind(TaskExecutor.class);
		bind(Executors.class).toInstance(new Executors() {

			@Override
			public ScheduledExecutorService initClusterTaskScheduler() throws PlatformException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void destroyClusterTaskScheduler() throws PlatformException {
				// TODO Auto-generated method stub
				
			}

			@Override
			public ExecutorService initClusterTaskExecutor(RejectedExecutionHandler handler) throws PlatformException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void destroyClusterTaskExecutor() throws PlatformException {
				// TODO Auto-generated method stub
				
			}

			@Override
			public ExecutorService initProcessingUnitExecutor(RejectedExecutionHandler handler) throws PlatformException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void destroyProcessingUnitExecutor() throws PlatformException {
				// TODO Auto-generated method stub
				
			}

			@Override
			public ScheduledExecutorService initProcessingUnitScheduler() throws PlatformException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void destroyProcessingUnitScheduler() throws PlatformException {
				// TODO Auto-generated method stub
				
			}
			
			
		});
		bind(Cluster.class);
		bind(Platform.class).to(PlatformImpl.class);
	}

}
