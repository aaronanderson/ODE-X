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

import static org.apache.ode.runtime.exec.platform.NodeImpl.CLUSTER_JAXB_CTX;
import static org.apache.ode.spi.exec.Node.CLUSTER_NAMESPACE;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import org.apache.ode.runtime.exec.cluster.xml.ClusterConfig;
import org.apache.ode.runtime.exec.platform.NodeImpl.ClusterId;
import org.apache.ode.runtime.exec.platform.NodeImpl.LocalNodeState;
import org.apache.ode.runtime.exec.platform.NodeImpl.NodeCheck;
import org.apache.ode.runtime.exec.platform.NodeImpl.NodeId;
import org.apache.ode.spi.exec.Platform.NodeStatus;
import org.apache.ode.spi.exec.Platform.NodeStatus.NodeState;

@Singleton
public class HealthCheck implements Runnable {

	@Inject
	ClusterConfig clusterConfig;

	@ClusterId
	String clusterId;

	@NodeId
	String nodeId;

	@PersistenceContext(unitName = "platform")
	private EntityManager pmgr;

	@NodeCheck
	TopicConnectionFactory topicConFactory;

	private TopicConnection topicConnection;
	private TopicSession nodeStatusSession;

	@NodeCheck
	private Topic nodeStatusTopic;

	@LocalNodeState
	AtomicReference<NodeState> localNodeState;

	private TopicPublisher publisher;
	private TopicSubscriber subscriber;

	private AtomicReference<Set<NodeStatusImpl>> nodeStatus = new AtomicReference<Set<NodeStatusImpl>>();
	private AtomicReference<Set<String>> onlineClusterNodes = new AtomicReference<Set<String>>();

	private org.apache.ode.runtime.exec.cluster.xml.HealthCheck config;

	private static final Logger log = Logger.getLogger(HealthCheck.class.getName());

	@PostConstruct
	public void init() throws Exception {
		topicConnection = topicConFactory.createTopicConnection();
		nodeStatusSession = topicConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
		publisher = nodeStatusSession.createPublisher(nodeStatusTopic);
		subscriber = nodeStatusSession.createSubscriber(nodeStatusTopic);
		topicConnection.start();
		this.config = clusterConfig.getHealthCheck();
		deleteStaleNodes();
	}

	@PreDestroy
	public void destroy() throws Exception {
		publisher.close();
		subscriber.close();
		nodeStatusSession.close();
		topicConnection.close();
	}

	public Set<NodeStatus> availableNodes() {
		return (Set<NodeStatus>) (Object) (nodeStatus.get());
	}

	public Set<String> onlineClusterNodes() {
		return onlineClusterNodes.get();
	}

	public void deleteStaleNodes() {

		// Delete stale nodes
		Calendar now = GregorianCalendar.getInstance();
		Calendar lifeTime = (Calendar) now.clone();
		Duration expiration = config.getNodeCleanup().negate();
		expiration.addTo(lifeTime);

		pmgr.getTransaction().begin();
		try {
			Query deleteStale = pmgr.createNamedQuery("deleteDeadNodes");
			deleteStale.setParameter("lifetime", lifeTime);
			deleteStale.executeUpdate();
			pmgr.getTransaction().commit();
		} catch (PersistenceException pe) {
			log.log(Level.SEVERE, "", pe);
			pmgr.getTransaction().rollback();
		}

	}

	@Override
	public synchronized void run() {
		try {// stop for nothing
			Calendar now = GregorianCalendar.getInstance();

			// Read in the existing node status
			Calendar lifeTime = (Calendar) now.clone();
			Duration expiration = config.getNodeCleanup().negate();
			expiration.addTo(lifeTime);
			Calendar active = (Calendar) now.clone();
			active.add(Calendar.MILLISECOND, (int) -config.getInactiveTimeout());

			HashSet<NodeStatusImpl> currentNodes = new HashSet<NodeStatusImpl>();
			HashSet<String> currentOnlineClusterNodes = new HashSet<String>();

			NodeStatusImpl local = null;
			pmgr.getTransaction().begin();
			try {
				local = pmgr.find(NodeStatusImpl.class, nodeId);
				if (local != null) {
					local.setState(localNodeState.get());
					local.setHeartBeat(now);
					pmgr.merge(local);
					// TODO check version with previous to detect nodeId conflict 
				} else {
					local = new NodeStatusImpl();
					local.setClusterId(clusterId);
					local.setNodeId(nodeId);
					local.setState(localNodeState.get());
					local.setHeartBeat(now);
					pmgr.persist(local);
				}
				pmgr.getTransaction().commit();
			} catch (PersistenceException pe) {
				log.log(Level.SEVERE, "", pe);
			}
			pmgr.clear();

			/*
			// cull out nodes that have not responded recently
			List<Node> nodes = null;
			try {
				Query hcheckQ = pmgr.createNamedQuery("healthCheck");
				hcheckQ.setParameter("lifetime", lifeTime);
				nodes = (List<Node>) hcheckQ.getResultList();
			} catch (PersistenceException pe) {
				log.log(Level.SEVERE, "", pe);
			}

			for (Node n : nodes) {
				NodeState nState = NodeState.ONLINE.equals(n.getState()) && n.getHeartBeat().compareTo(active) >= 0 ? NodeState.ONLINE : NodeState.OFFLINE;
				NodeStatusImpl status = new NodeStatusImpl(n.getClusterId(), n.getNodeId(), nState);
				currentNodes.add(status);
				if (clusterId.equalsIgnoreCase(n.getClusterId()) && NodeState.ONLINE.equals(nState)) {
					currentOnlineClusterNodes.add(n.getNodeId());
				}
			}
			pmgr.clear();
			*/
			org.apache.ode.runtime.exec.cluster.xml.NodeStatus xmlNodeStatus = convert(local);
			BytesMessage message = nodeStatusSession.createBytesMessage();
			Marshaller marshaller = CLUSTER_JAXB_CTX.createMarshaller();
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			marshaller.marshal(new JAXBElement(new QName(CLUSTER_NAMESPACE, "NodeStatus"), org.apache.ode.runtime.exec.cluster.xml.NodeStatus.class,
					xmlNodeStatus), bos);
			message.writeBytes(bos.toByteArray());
			publisher.publish(message);

			while (true) {
				try {
					message = (BytesMessage) subscriber.receive(0l);
					if (message == null) {
						break;
					}
					byte[] payload = new byte[(int) message.getBodyLength()];
					message.readBytes(payload);
					Unmarshaller umarshaller = CLUSTER_JAXB_CTX.createUnmarshaller();
					JAXBElement<org.apache.ode.runtime.exec.cluster.xml.NodeStatus> element = umarshaller.unmarshal(new StreamSource(new ByteArrayInputStream(
							payload)), org.apache.ode.runtime.exec.cluster.xml.NodeStatus.class);
					xmlNodeStatus = element.getValue();
					currentNodes.add(convert(xmlNodeStatus));
				} catch (JMSException e) {
					log.log(Level.SEVERE, "", e);
				}
			}

			for (NodeStatusImpl n : currentNodes) {
				NodeState nState = NodeState.ONLINE.equals(n.state()) && n.getHeartBeat().compareTo(active) >= 0 ? NodeState.ONLINE : NodeState.OFFLINE;
				if (clusterId.equalsIgnoreCase(n.clusterId()) && NodeState.ONLINE.equals(nState)) {
					currentOnlineClusterNodes.add(n.nodeId());
				}
			}

			nodeStatus.set(Collections.unmodifiableSet(currentNodes));
			onlineClusterNodes.set(Collections.unmodifiableSet(currentOnlineClusterNodes));
		} catch (Throwable t) {
			log.log(Level.SEVERE, "", t);
		}
	}

	public static org.apache.ode.runtime.exec.cluster.xml.NodeStatus convert(NodeStatusImpl status) {
		org.apache.ode.runtime.exec.cluster.xml.NodeStatus xmlNodeStatus = new org.apache.ode.runtime.exec.cluster.xml.NodeStatus();
		xmlNodeStatus.setClusterId(status.clusterId());
		xmlNodeStatus.setNodeId(status.nodeId());
		xmlNodeStatus.setState(org.apache.ode.runtime.exec.cluster.xml.NodeState.valueOf(status.state().toString()));
		xmlNodeStatus.setHeartbeat(status.getHeartBeat());
		return xmlNodeStatus;
	}

	public static NodeStatusImpl convert(org.apache.ode.runtime.exec.cluster.xml.NodeStatus xmlNodeStatus) {
		NodeStatusImpl nodeStatus = new NodeStatusImpl();
		nodeStatus.setClusterId(xmlNodeStatus.getClusterId());
		nodeStatus.setNodeId(xmlNodeStatus.getNodeId());
		nodeStatus.setState(NodeState.valueOf(xmlNodeStatus.getState().toString()));
		nodeStatus.setHeartBeat(xmlNodeStatus.getHeartbeat());
		return nodeStatus;
	}
}
