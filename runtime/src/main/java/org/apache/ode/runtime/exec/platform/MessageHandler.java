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

import static org.apache.ode.runtime.exec.platform.NodeImpl.PLATFORM_JAXB_CTX;
import static org.apache.ode.spi.exec.Node.CLUSTER_NAMESPACE;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.jms.BytesMessage;
import javax.jms.IllegalStateException;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.IllegalStateException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import org.apache.ode.runtime.exec.cluster.xml.ClusterConfig;
import org.apache.ode.runtime.exec.platform.NodeImpl.ClusterId;
import org.apache.ode.runtime.exec.platform.NodeImpl.MessageCheck;
import org.apache.ode.runtime.exec.platform.NodeImpl.NodeId;
import org.apache.ode.spi.exec.Message;
import org.apache.ode.spi.exec.Message.ClusterListener;
import org.apache.ode.spi.exec.Message.LogLevel;
import org.apache.ode.spi.exec.Message.MessageEvent;
import org.apache.ode.spi.exec.Message.MessageListener;
import org.apache.ode.spi.exec.Message.NodeListener;
import org.apache.ode.spi.exec.Message.TaskListener;
import org.apache.ode.spi.exec.Node;

@Singleton
public class MessageHandler implements Runnable {

	public static final String MSG_MQ_ORIG_CLUSTER = "ODE_ORIG_CLUSTER";
	public static final String MSG_MQ_ORIG_NODE = "ODE_ORIG_NODE";
	public static final String MSG_MQ_TASK = "ODE_TASK";
	public static final String MSG_MQ_LEVEL = "ODE_MSG_LEVEL";

	@Inject
	ClusterConfig clusterConfig;

	@ClusterId
	String clusterId;

	@NodeId
	String nodeId;

	@PersistenceContext(unitName = "platform")
	private EntityManager pmgr;

	@MessageCheck
	private TopicConnectionFactory msgConnectionFactory;

	@MessageCheck
	private Topic msgTopic;

	private TopicConnection pollMsgTopicConnection;
	private TopicSession pollMsgSession;
	private MessageProducer producer;
	private MessageConsumer consumer;

	private CopyOnWriteArrayList<MessageListener> listeners = new CopyOnWriteArrayList<MessageListener>();

	org.apache.ode.runtime.exec.cluster.xml.MessageCheck config;

	private static final Logger log = Logger.getLogger(MessageHandler.class.getName());

	@PostConstruct
	public void init() {
		this.config = clusterConfig.getMessageCheck();
		try {
			pollMsgTopicConnection = msgConnectionFactory.createTopicConnection();
			pollMsgSession = pollMsgTopicConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
			producer = pollMsgSession.createProducer(msgTopic);
			consumer = pollMsgSession.createConsumer(msgTopic);
		} catch (JMSException e) {
			log.log(Level.SEVERE, "", e);
		}
	}

	@PreDestroy
	public void destroy() {
		try {
			pollMsgTopicConnection.close();
		} catch (JMSException e) { //don't care about JMS errors on closure
		}
	}

	public void addListener(MessageListener listener) {
		listeners.add(listener);
	}

	public void removeListener(MessageListener listener) {
		listeners.remove(listener);
	}

	public void log(LogLevel level, int code, String messageText, String targetNodeId, String targetClusterId, String taskId) {
		try {
			org.apache.ode.spi.exec.platform.xml.Message xmlMessage = new org.apache.ode.spi.exec.platform.xml.Message();
			xmlMessage.setLevel(org.apache.ode.spi.exec.platform.xml.LogLevel.valueOf(level.toString()));
			xmlMessage.setCode(BigInteger.valueOf(code));
			xmlMessage.setValue(messageText);
			xmlMessage.setTimestamp(Calendar.getInstance());

			BytesMessage message = pollMsgSession.createBytesMessage();
			message.setStringProperty(Node.NODE_MQ_PROP_NODE, targetNodeId);
			message.setStringProperty(Node.NODE_MQ_PROP_CLUSTER, targetClusterId);
			message.setStringProperty(MSG_MQ_ORIG_NODE, nodeId);
			message.setStringProperty(MSG_MQ_ORIG_CLUSTER, clusterId);
			message.setStringProperty(MSG_MQ_TASK, taskId);

			Marshaller marshaller = NodeImpl.PLATFORM_JAXB_CTX.createMarshaller();
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			marshaller.marshal(new JAXBElement(new QName(CLUSTER_NAMESPACE, "Message"), org.apache.ode.spi.exec.platform.xml.Message.class, xmlMessage), bos);
			message.writeBytes(bos.toByteArray());
			producer.send(message);
		} catch (Exception je) {
			log.log(Level.SEVERE, "", je);
		}

	}

	public static void log(MessageImpl m, LogLevel logLevel, java.util.Queue<org.apache.ode.spi.exec.platform.xml.Message> msgQueue,
			TopicSession msgUpdateSession, String correlationId, TopicPublisher msgUpdatePublisher) {
		try {
			org.apache.ode.spi.exec.platform.xml.Message xmlMessage = convert(m);
			if (logLevel.ordinal() >= xmlMessage.getLevel().ordinal()) {
				msgQueue.add(xmlMessage);

				BytesMessage jmsMessage = msgUpdateSession.createBytesMessage();
				Marshaller marshaller = PLATFORM_JAXB_CTX.createMarshaller();
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				marshaller.marshal(new JAXBElement(new QName(CLUSTER_NAMESPACE, "Message"), org.apache.ode.spi.exec.platform.xml.Message.class, xmlMessage),
						bos);
				jmsMessage.writeBytes(bos.toByteArray());
				jmsMessage.setJMSCorrelationID(correlationId);
				msgUpdatePublisher.publish(jmsMessage);
			}
		} catch (Exception je) {
			log.log(Level.SEVERE, "", je);
		}
	}

	public static org.apache.ode.spi.exec.platform.xml.Message convert(MessageImpl message) {
		org.apache.ode.spi.exec.platform.xml.Message xmlMessage = new org.apache.ode.spi.exec.platform.xml.Message();
		xmlMessage.setLevel(org.apache.ode.spi.exec.platform.xml.LogLevel.fromValue(message.level().toString()));
		xmlMessage.setCode(BigInteger.valueOf(message.code()));
		xmlMessage.setTimestamp(Calendar.getInstance());
		xmlMessage.setValue(message.message());
		return xmlMessage;
	}

	@Override
	public synchronized void run() {
		while (true) {
			try {
				BytesMessage message = (BytesMessage) consumer.receive(config.getFrequency());
				if (message == null) {
					break;
				}
				String targetNodeId = message.getStringProperty(Node.NODE_MQ_PROP_NODE);
				String targetClusterId = message.getStringProperty(Node.NODE_MQ_PROP_CLUSTER);
				String origNodeId = message.getStringProperty(MSG_MQ_ORIG_NODE);
				String origClusterId = message.getStringProperty(MSG_MQ_ORIG_CLUSTER);
				String taskId = message.getStringProperty(MSG_MQ_TASK);
				int logLevel = Integer.parseInt(message.getStringProperty(MSG_MQ_LEVEL));

				Set<MessageListener> interested = new HashSet<MessageListener>();
				for (MessageListener l : listeners) {
					LogLevel ll = l.levelFilter();
					if (l instanceof TaskListener) {
						TaskListener l2 = (TaskListener) l;

					} else if (l instanceof ClusterListener) {
						ClusterListener l2 = (ClusterListener) l;
						if (targetClusterId.equals(l2.clusterIdFilter()) && ll.ordinal() <= logLevel) {
							interested.add(l);
						}

					} else if (l instanceof NodeListener) {
						NodeListener l2 = (NodeListener) l;
						if (targetNodeId.equals(l2.nodeIdFilter()) && ll.ordinal() <= logLevel) {
							interested.add(l);
						}
					}
				}
				if (interested.size() > 0) {
					try {
						byte[] payload = new byte[(int) message.getBodyLength()];
						message.readBytes(payload);
						Unmarshaller umarshaller = PLATFORM_JAXB_CTX.createUnmarshaller();
						JAXBElement<org.apache.ode.spi.exec.platform.xml.Message> element = umarshaller.unmarshal(new StreamSource(new ByteArrayInputStream(
								payload)), org.apache.ode.spi.exec.platform.xml.Message.class);
						org.apache.ode.spi.exec.platform.xml.Message xmlMessage = element.getValue();
						MessageEventImpl event = convert(xmlMessage, origNodeId, origClusterId);
						for (MessageListener l : interested) {
							l.message(event);
						}
					} catch (JAXBException je) {
						log.log(Level.SEVERE, "", je);
					}

				}
			} catch (Throwable e) {
				if (e instanceof IllegalStateException || e.getCause() instanceof InterruptedException) {
					break;
				} else {
					log.log(Level.SEVERE, "", e);
				}
			}
		}

	}

	public static MessageEventImpl convert(org.apache.ode.spi.exec.platform.xml.Message xmlMessage, String origNodeId, String origClusterId) {
		MessageImpl message = new MessageImpl();
		message.setLevel(xmlMessage.getLevel().toString());
		message.setCode(xmlMessage.getCode().intValue());
		message.setMessage(xmlMessage.getValue());
		message.setTimestamp(xmlMessage.getTimestamp().getTime());
		MessageEventImpl event = new MessageEventImpl(origClusterId, origNodeId, message);
		return event;
	}

	public static class MessageEventImpl implements MessageEvent {
		private final String clusterId;
		private final String nodeId;
		private final MessageImpl message;

		public MessageEventImpl(String clusterId, String nodeId, MessageImpl message) {
			this.clusterId = clusterId;
			this.nodeId = nodeId;
			this.message = message;
		}

		@Override
		public Message message() {
			return message;
		}

		@Override
		public String clusterId() {
			return clusterId;
		}

		@Override
		public String nodeId() {
			return nodeId;
		}

	}

}
