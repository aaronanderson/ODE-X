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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Provider;
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
import org.apache.ode.spi.exec.Node;


//Good info https://community.jboss.org/wiki/ShouldICacheJMSConnectionsAndJMSSessions?_sscc=t
public class AQJMSModule extends JMSModule {

	protected void configure() {
		super.configure();
		bind(AQBroker.class);
	}

	@Override
	protected Class<? extends Provider<TopicConnectionFactory>> getHealthCheckFactory() {

		return TopicConnectionFactoryProvider.class;
	}

	@Override
	protected Class<? extends Provider<Topic>> getHealthCheckTopic() {
		class HealthCheckTopic implements Provider<Topic> {
			@Inject
			AQBroker broker;

			@Override
			public Topic get() {
				return broker.hcTopic;
			}

		}
		return HealthCheckTopic.class;
	}

	@Override
	protected Class<? extends Provider<QueueConnectionFactory>> getTaskFactory() {

		return QueueConnectionFactoryProvider.class;
	}

	@Override
	protected Class<? extends Provider<Queue>> getTaskQueue() {

		class TaskQueue implements Provider<Queue> {
			@Inject
			AQBroker broker;

			@Override
			public Queue get() {
				return broker.taskQueue;
			}

		}
		return TaskQueue.class;
	}

	@Override
	protected Class<? extends Provider<QueueConnectionFactory>> getMessageFactory() {

		return QueueConnectionFactoryProvider.class;
	}

	@Override
	protected Class<? extends Provider<Topic>> getMessageTopic() {

		class MessageTopic implements Provider<Topic> {
			@Inject
			AQBroker broker;

			@Override
			public Topic get() {
				return broker.msgTopic;
			}

		}

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

	@Singleton
	public static class AQBroker {
		public static final String BROKER_URL = "vm://localhost";
		ActiveMQTopic hcTopic;
		ActiveMQQueue taskQueue;
		ActiveMQTopic msgTopic;

		BrokerService broker;

		@PostConstruct
		public void init() {
			try {
				broker = new BrokerService();
				broker.setPersistent(false);
				broker.setUseJmx(false);
				broker.addConnector(BROKER_URL);
				hcTopic = new ActiveMQTopic(Node.NODE_MQ_NAME_HEALTHCHECK);
				taskQueue = new ActiveMQQueue(Node.NODE_MQ_NAME_TASK);
				msgTopic = new ActiveMQTopic(Node.NODE_MQ_NAME_MESSAGE);
				broker.setDestinations(new ActiveMQDestination[] { hcTopic, taskQueue, msgTopic });
				broker.start();

			} catch (Exception e) {

			}

		}

		@PreDestroy
		public void destroy() {
			try {
				broker.stop();
			} catch (Exception e) {

			}

		}

		ActiveMQConnectionFactory newFactory() {
			ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(BROKER_URL);
			factory = new ActiveMQConnectionFactory(BROKER_URL);
			factory.setObjectMessageSerializationDefered(true);
			factory.setCopyMessageOnSend(false);
			return factory;
		}

	}

}
