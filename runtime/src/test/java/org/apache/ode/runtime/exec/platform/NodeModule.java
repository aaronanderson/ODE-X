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

import java.lang.reflect.Field;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.ode.runtime.exec.cluster.xml.ClusterConfig;
import org.apache.ode.runtime.exec.platform.NodeImpl.ClusterConfigProvider;
import org.apache.ode.runtime.exec.platform.NodeImpl.ClusterId;
import org.apache.ode.runtime.exec.platform.NodeImpl.LocalNodeState;
import org.apache.ode.runtime.exec.platform.NodeImpl.LocalNodeStateProvider;
import org.apache.ode.runtime.exec.platform.NodeImpl.NodeId;
import org.apache.ode.runtime.exec.platform.task.TaskExecutor;
import org.apache.ode.runtime.exec.platform.task.TaskPoll;
import org.apache.ode.spi.exec.Executors;
import org.apache.ode.spi.exec.Node;
import org.apache.ode.spi.exec.Platform;
import org.apache.ode.spi.exec.PlatformException;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.MembersInjector;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

public class NodeModule extends AbstractModule {
	protected void configure() {
		bindListener(Matchers.any(), new NodeTypeListener());
		bind(AtomicReference.class).annotatedWith(LocalNodeState.class).toProvider(LocalNodeStateProvider.class);
		bind(ClusterConfig.class).toProvider(ClusterConfigProvider.class);
		bind(HealthCheck.class);
		bind(TaskPoll.class);
		bind(TaskExecutor.class);
		bind(MessageHandler.class);
		bind(Executors.class).toInstance(getExecutors());
		bind(Node.class).to(NodeImpl.class);
		bind(Platform.class).to(PlatformImpl.class);
	}

	public static class NodeTypeListener implements TypeListener {
		public <T> void hear(TypeLiteral<T> typeLiteral, TypeEncounter<T> typeEncounter) {
			for (Field field : typeLiteral.getRawType().getDeclaredFields()) {
				if (field.getType() == String.class) {
					if (field.isAnnotationPresent(NodeId.class)) {
						typeEncounter.register(new NodeMembersInjector(field, typeEncounter.getProvider(Key.get(field.getType(), NodeId.class))));
					} else if (field.isAnnotationPresent(ClusterId.class)) {
						typeEncounter.register(new NodeMembersInjector(field, typeEncounter.getProvider(Key.get(field.getType(), ClusterId.class))));
					}

				} else if (field.getType() == AtomicReference.class) {
					if (field.isAnnotationPresent(LocalNodeState.class)) {
						typeEncounter.register(new NodeMembersInjector(field, typeEncounter.getProvider(Key.get(field.getType(), LocalNodeState.class))));
					}
				}

			}
		}
	}

	public static class NodeMembersInjector<T> implements MembersInjector<T> {
		private final Field field;
		private final Provider provider;

		NodeMembersInjector(Field field, Provider provider) {
			this.field = field;
			this.provider = provider;
			field.setAccessible(true);
		}

		public void injectMembers(T t) {
			try {
				field.set(t, provider.get());
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
	}

	protected Executors getExecutors() {
		return new DefaultExecutors();
	}

	public static class DefaultExecutors implements Executors {
		private ScheduledExecutorService clusterScheduler;
		private ThreadPoolExecutor clusterExec;

		protected int getMinThreads() {
			return 30;
		}

		protected int getMaxThreads() {
			return 30;
		}

		protected long getThreadTimeout() {
			return 30;
		}

		protected int getQueueSize() {
			return 300;
		}

		public int getShutdownWaitTime() {
			return 30;
		}

		@Override
		public ScheduledExecutorService initClusterTaskScheduler() throws PlatformException {
			clusterScheduler = new ScheduledThreadPoolExecutor(5, new ThreadFactory() {

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
		public void destroyClusterTaskScheduler() throws PlatformException {
			clusterScheduler.shutdownNow();
		}

		@Override
		public ExecutorService initClusterTaskExecutor(RejectedExecutionHandler handler) throws PlatformException {
			BlockingQueue<Runnable> taskQueue = new ArrayBlockingQueue<Runnable>(getQueueSize());

			clusterExec = new ThreadPoolExecutor(getMinThreads(), getMaxThreads(), getThreadTimeout(), TimeUnit.SECONDS, taskQueue, new ThreadFactory() {

				private final ThreadFactory factory = java.util.concurrent.Executors.defaultThreadFactory();
				private long id = 0;

				@Override
				public Thread newThread(Runnable r) {
					Thread t = factory.newThread(r);
					t.setName("ODE-X Cluster Task Executor - " + ++id);
					t.setDaemon(true);
					return t;
				}
			}, handler);
			clusterExec.allowCoreThreadTimeOut(true);
			return clusterExec;
		}

		@Override
		public void destroyClusterTaskExecutor() throws PlatformException {
			if (clusterExec != null) {
				clusterExec.shutdown();
				try {
					clusterExec.awaitTermination(getShutdownWaitTime(), TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					throw new PlatformException(e);
				}
			}

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

		@Override
		public ExecutorService initIOExecutor(RejectedExecutionHandler handler) throws PlatformException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void destroyIOExecutor() throws PlatformException {
			// TODO Auto-generated method stub
			
		}

	}
}
