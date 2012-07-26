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
package org.apache.ode.runtime.exec.modules;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Qualifier;
import javax.inject.Singleton;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.Topic;
import javax.jms.TopicConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.ode.runtime.exec.cluster.xml.ClusterConfig;
import org.apache.ode.runtime.exec.modules.JMSModule.JMSSessionMembersInjector;
import org.apache.ode.runtime.exec.modules.JMSModule.JMSTypeListener;
import org.apache.ode.runtime.exec.modules.NodeModule.NodeTypeListener;
import org.apache.ode.runtime.exec.platform.HealthCheck;
import org.apache.ode.runtime.exec.platform.NodeImpl.ClusterConfigProvider;
import org.apache.ode.runtime.exec.platform.NodeImpl.ClusterId;
import org.apache.ode.runtime.exec.platform.NodeImpl.LocalNodeState;
import org.apache.ode.runtime.exec.platform.NodeImpl.LocalNodeStateProvider;
import org.apache.ode.runtime.exec.platform.NodeImpl.MessageCheck;
import org.apache.ode.runtime.exec.platform.NodeImpl.NodeCheck;
import org.apache.ode.runtime.exec.platform.NodeImpl.NodeId;
import org.apache.ode.runtime.exec.platform.NodeImpl.TaskCheck;
import org.apache.ode.spi.exec.Node;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

public abstract class AQJMSModule extends JMSModule {

	private static final Logger log = Logger.getLogger(AQJMSModule.class.getName());

	@Override
	protected Class<? extends Provider<TopicConnectionFactory>> getHealthCheckFactory() {

		return TopicConnectionFactoryProvider.class;
	}

	static class HealthCheckTopic implements Provider<Topic> {
		@Inject
		AQBroker broker;

		@Override
		public Topic get() {
			return broker.healthCheckTopic();
		}

	}

	@Override
	protected Class<? extends Provider<Topic>> getHealthCheckTopic() {

		return HealthCheckTopic.class;
	}

	@Override
	protected Class<? extends Provider<QueueConnectionFactory>> getTaskFactory() {

		return QueueConnectionFactoryProvider.class;
	}

	static class TaskQueue implements Provider<Queue> {
		@Inject
		AQBroker broker;

		@Override
		public Queue get() {
			return broker.taskQueue();
		}

	}

	@Override
	protected Class<? extends Provider<Queue>> getTaskQueue() {

		return TaskQueue.class;
	}

	@Override
	protected Class<? extends Provider<TopicConnectionFactory>> getMessageFactory() {

		return TopicConnectionFactoryProvider.class;
	}

	static class MessageTopic implements Provider<Topic> {
		@Inject
		AQBroker broker;

		@Override
		public Topic get() {
			return broker.messageTopic();
		}

	}

	@Override
	protected Class<? extends Provider<Topic>> getMessageTopic() {

		return MessageTopic.class;
	}

	@Singleton
	public static class QueueConnectionFactoryProvider implements Provider<QueueConnectionFactory> {

		@Inject
		AQBroker broker;

		@Override
		public QueueConnectionFactory get() {
			return broker.newFactory();
		}

	}

	@Singleton
	public static class TopicConnectionFactoryProvider implements Provider<TopicConnectionFactory> {

		@Inject
		AQBroker broker;

		@Override
		public TopicConnectionFactory get() {
			return broker.newFactory();
		}

	}

	@Qualifier
	@java.lang.annotation.Target({ ElementType.FIELD, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface AQBrokerURL {

	}

	public static interface AQBroker {
		ActiveMQTopic healthCheckTopic();

		ActiveMQQueue taskQueue();

		ActiveMQTopic messageTopic();

		ActiveMQConnectionFactory newFactory();
	}

	public static class AQJMSTypeListener implements TypeListener {
		public <T> void hear(TypeLiteral<T> typeLiteral, TypeEncounter<T> typeEncounter) {
			for (Field field : typeLiteral.getRawType().getDeclaredFields()) {
				if (field.getType() == String.class && field.isAnnotationPresent(AQBrokerURL.class)) {
					typeEncounter.register(new JMSSessionMembersInjector(field, typeEncounter.getProvider(Key.get(field.getType(), AQBrokerURL.class))));
				}

			}
		}
	}

	@Singleton
	public static class VMAQBroker implements AQBroker {
		//public static final String BROKER_URL = "vm://localhost";
		ActiveMQTopic hcTopic;
		ActiveMQQueue taskQueue;
		ActiveMQTopic msgTopic;

		BrokerService broker;

		@AQBrokerURL
		String aqBroker;

		@PostConstruct
		public void init() {
			try {
				/*			broker = new BrokerService();
							broker.setPersistent(false);
							broker.setUseJmx(false);
							broker.addConnector(aqBroker);*/
				hcTopic = new ActiveMQTopic(Node.NODE_MQ_NAME_HEALTHCHECK);
				taskQueue = new ActiveMQQueue(Node.NODE_MQ_NAME_TASK);
				msgTopic = new ActiveMQTopic(Node.NODE_MQ_NAME_MESSAGE);
				//broker.setDestinations(new ActiveMQDestination[] { hcTopic, taskQueue, msgTopic });
				//broker.start();

			} catch (Exception e) {
				log.log(Level.SEVERE, "", e);
			}

		}

		/*
				@PreDestroy
				public void destroy() {
					try {
						broker.stop();
					} catch (Exception e) {
						log.log(Level.SEVERE, "", e);
					}

				}*/

		@Override
		public ActiveMQTopic healthCheckTopic() {
			return hcTopic;
		}

		@Override
		public ActiveMQQueue taskQueue() {
			return taskQueue;
		}

		@Override
		public ActiveMQTopic messageTopic() {
			return msgTopic;
		}

		@Override
		public ActiveMQConnectionFactory newFactory() {
			ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(aqBroker);
			factory.setObjectMessageSerializationDefered(true);
			factory.setCopyMessageOnSend(false);
			return factory;
		}

	}

	public static class AQVMJMSModule extends AQJMSModule {
		String aqBrokerURL;

		public AQVMJMSModule(String aqBrokerURL) {
			this.aqBrokerURL = aqBrokerURL;
		}

		@Override
		protected void configure() {
			super.configure();
			bind(AQBroker.class).to(VMAQBroker.class);
			bindListener(Matchers.any(), new AQJMSTypeListener());
			bindConstant().annotatedWith(AQBrokerURL.class).to(aqBrokerURL);

		}
	}

}
