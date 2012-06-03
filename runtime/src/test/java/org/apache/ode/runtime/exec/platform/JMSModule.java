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

import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;

import org.apache.ode.runtime.exec.platform.JMSUtil.QueueImpl;
import org.apache.ode.runtime.exec.platform.JMSUtil.SessionImpl;
import org.apache.ode.runtime.exec.platform.JMSUtil.TopicImpl;
import org.apache.ode.runtime.exec.platform.NodeImpl.NodeCheck;
import org.apache.ode.runtime.exec.platform.NodeImpl.TaskCheck;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.MembersInjector;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

//Good info https://community.jboss.org/wiki/ShouldICacheJMSConnectionsAndJMSSessions?_sscc=t
public class JMSModule extends AbstractModule {

	protected void configure() {

		bindListener(Matchers.any(), new JMSTypeListener());

		Session hSession = getHealthCheckSession();
		Topic hTopic = getHealthCheckTopic();
		bind(Session.class).annotatedWith(NodeCheck.class).toInstance(hSession);
		bind(Topic.class).annotatedWith(NodeCheck.class).toInstance(hTopic);

		Session taskSession = getTaskSession();
		Queue taskQueue = getTaskQueue();
		bind(Session.class).annotatedWith(TaskCheck.class).toInstance(taskSession);
		bind(Queue.class).annotatedWith(TaskCheck.class).toInstance(taskQueue);

	}

	protected Session getHealthCheckSession() {
		return new SessionImpl();
	}

	protected Topic getHealthCheckTopic() {
		return new TopicImpl("ODE_HEALTHCHECK");
	}

	protected Session getTaskSession() {
		return new SessionImpl();
	}

	protected Queue getTaskQueue() {
		return new QueueImpl("ODE_TASK");
	}

	public static class JMSTypeListener implements TypeListener {
		public <T> void hear(TypeLiteral<T> typeLiteral, TypeEncounter<T> typeEncounter) {
			for (Field field : typeLiteral.getRawType().getDeclaredFields()) {
				if (field.getType() == Session.class || field.getType() == Topic.class || field.getType() == Queue.class) {
					if (field.isAnnotationPresent(NodeCheck.class)) {
						typeEncounter.register(new JMSSessionMembersInjector(field, typeEncounter.getProvider(Key.get(field.getType(), NodeCheck.class))));
					} else if (field.isAnnotationPresent(TaskCheck.class)) {
						typeEncounter.register(new JMSSessionMembersInjector(field, typeEncounter.getProvider(Key.get(field.getType(), TaskCheck.class))));
					}

				} /*else if (field.getType() == Topic.class) {
					if (field.isAnnotationPresent(NodeCheck.class)) {
						typeEncounter.register(typeEncounter.getMembersInjector(typeLiteral));
					}
					}*/

			}
		}
	}

	public static class JMSSessionMembersInjector<T> implements MembersInjector<T> {
		private final Field field;
		private final Provider provider;

		JMSSessionMembersInjector(Field field, Provider provider) {
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

}
