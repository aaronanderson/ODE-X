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

import javax.inject.Provider;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.Topic;
import javax.jms.TopicConnectionFactory;

import org.apache.ode.runtime.exec.platform.JMSUtil.QueueConnectionFactoryImpl;
import org.apache.ode.runtime.exec.platform.JMSUtil.QueueImpl;
import org.apache.ode.runtime.exec.platform.JMSUtil.TopicConnectionFactoryImpl;
import org.apache.ode.runtime.exec.platform.JMSUtil.TopicImpl;
import org.apache.ode.runtime.exec.platform.NodeImpl.MessageCheck;
import org.apache.ode.runtime.exec.platform.NodeImpl.NodeCheck;
import org.apache.ode.runtime.exec.platform.NodeImpl.TaskCheck;
import org.apache.ode.spi.exec.Node;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.MembersInjector;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

//Good info https://community.jboss.org/wiki/ShouldICacheJMSConnectionsAndJMSSessions?_sscc=t
public class JMSModule extends AbstractModule {

	protected void configure() {

		bindListener(Matchers.any(), new JMSTypeListener());

		bind(TopicConnectionFactory.class).annotatedWith(NodeCheck.class).toProvider(getHealthCheckFactory());
		bind(Topic.class).annotatedWith(NodeCheck.class).toProvider(getHealthCheckTopic());

		bind(QueueConnectionFactory.class).annotatedWith(TaskCheck.class).toProvider(getTaskFactory());
		bind(Queue.class).annotatedWith(TaskCheck.class).toProvider(getTaskQueue());

		bind(QueueConnectionFactory.class).annotatedWith(MessageCheck.class).toProvider(getMessageFactory());
		bind(Topic.class).annotatedWith(MessageCheck.class).toProvider(getMessageTopic());
	}

	protected Class<? extends Provider<TopicConnectionFactory>> getHealthCheckFactory() {
		class HealthCheckTopicFactory implements Provider<TopicConnectionFactory> {
			TopicConnectionFactory impl = new TopicConnectionFactoryImpl();

			@Override
			public TopicConnectionFactory get() {
				return impl;
			}

		}
		return HealthCheckTopicFactory.class;
	}

	protected Class<? extends Provider<Topic>> getHealthCheckTopic() {
		class HealthCheckTopic implements Provider<Topic> {
			Topic impl = new TopicImpl(Node.NODE_MQ_NAME_HEALTHCHECK);

			@Override
			public Topic get() {
				return impl;
			}

		}
		return HealthCheckTopic.class;
	}

	protected Class<? extends Provider<QueueConnectionFactory>> getTaskFactory() {
		class TaskFactory implements Provider<QueueConnectionFactory> {
			QueueConnectionFactory impl = new QueueConnectionFactoryImpl();

			@Override
			public QueueConnectionFactory get() {
				return impl;
			}

		}
		return TaskFactory.class;
	}

	protected Class<? extends Provider<Queue>> getTaskQueue() {
		class TaskQueue implements Provider<Queue> {
			Queue impl = new QueueImpl(Node.NODE_MQ_NAME_TASK);

			@Override
			public Queue get() {
				return impl;
			}

		}
		return TaskQueue.class;
	}

	protected Class<? extends Provider<QueueConnectionFactory>> getMessageFactory() {
		class MessageFactory implements Provider<QueueConnectionFactory> {
			QueueConnectionFactory impl = new QueueConnectionFactoryImpl();

			@Override
			public QueueConnectionFactory get() {
				return impl;
			}

		}
		return MessageFactory.class;
	}

	protected Class<? extends Provider<Topic>> getMessageTopic() {

		class MessageTopic implements Provider<Topic> {
			Topic impl = new TopicImpl(Node.NODE_MQ_NAME_MESSAGE);

			@Override
			public Topic get() {
				return impl;
			}

		}

		return MessageTopic.class;
	}

	public static class JMSTypeListener implements TypeListener {
		public <T> void hear(TypeLiteral<T> typeLiteral, TypeEncounter<T> typeEncounter) {
			for (Field field : typeLiteral.getRawType().getDeclaredFields()) {
				if (field.getType() == TopicConnectionFactory.class || field.getType() == QueueConnectionFactory.class || field.getType() == Topic.class
						|| field.getType() == Queue.class) {
					if (field.isAnnotationPresent(NodeCheck.class)) {
						typeEncounter.register(new JMSSessionMembersInjector(field, typeEncounter.getProvider(Key.get(field.getType(), NodeCheck.class))));
					} else if (field.isAnnotationPresent(TaskCheck.class)) {
						typeEncounter.register(new JMSSessionMembersInjector(field, typeEncounter.getProvider(Key.get(field.getType(), TaskCheck.class))));
					} else if (field.isAnnotationPresent(MessageCheck.class)) {
						typeEncounter.register(new JMSSessionMembersInjector(field, typeEncounter.getProvider(Key.get(field.getType(), MessageCheck.class))));
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
