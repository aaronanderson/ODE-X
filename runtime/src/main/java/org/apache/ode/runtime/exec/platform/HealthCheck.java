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

import static org.apache.ode.runtime.exec.platform.Cluster.CLUSTER_JAXB_CTX;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.Duration;

import org.apache.ode.runtime.exec.platform.Cluster.NodeCheck;
import org.apache.ode.spi.exec.NodeStatus;
import org.apache.ode.spi.exec.NodeStatus.NodeState;

public class HealthCheck implements Runnable {

	private String clusterId;
	private String nodeId;

	@PersistenceContext(unitName = "platform")
	private EntityManager pmgr;

	@NodeCheck
	private Session nodeStatusSession;

	@NodeCheck
	private Topic nodeStatusTopic;

	private MessageProducer producer;
	private MessageConsumer consumer;

	private AtomicReference<Set<NodeStatusImpl>> nodeStatus = new AtomicReference<Set<NodeStatusImpl>>();
	private AtomicReference<Set<String>> onlineClusterNodes = new AtomicReference<Set<String>>();

	private org.apache.ode.runtime.exec.cluster.xml.HealthCheck config;
	AtomicReference<NodeState> localNodeState;

	private static final Logger log = Logger.getLogger(HealthCheck.class.getName());

	@PostConstruct
	public void init() throws Exception {
		producer = nodeStatusSession.createProducer(nodeStatusTopic);
		consumer = nodeStatusSession.createConsumer(nodeStatusTopic);
	}

	public void config(String clusterId, String nodeId, AtomicReference<NodeState> localNodeState, org.apache.ode.runtime.exec.cluster.xml.HealthCheck config) {
		this.clusterId = clusterId;
		this.nodeId = nodeId;
		this.localNodeState = localNodeState;
		this.config = config;
		deleteStaleNodes();
	}

	@PreDestroy
	public void destroy() throws Exception {
		producer.close();
		consumer.close();
	}

	public Set<NodeStatus> availableNodes() {
		return new HashSet<NodeStatus>(nodeStatus.get());
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
		}

	}

	public static class NodeStatusImpl extends org.apache.ode.runtime.exec.cluster.xml.NodeStatus implements NodeStatus {

		public NodeStatusImpl() {

		}

		public NodeStatusImpl(String clusterId, String nodeId, NodeState state) {
			this.clusterId = clusterId;
			this.nodeId = nodeId;
			this.state = org.apache.ode.runtime.exec.cluster.xml.NodeState.fromValue(state.name());
		}

		@Override
		public String clusterId() {
			return clusterId;
		}

		@Override
		public String nodeId() {
			return nodeId;
		}

		@Override
		public NodeState state() {
			return NodeState.valueOf(state.name());
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof NodeStatusImpl) {
				NodeStatusImpl n1 = (NodeStatusImpl) obj;
				if (n1.clusterId.equals(clusterId) && n1.nodeId.equals(nodeId)) {
					return true;
				}
			}
			return false;
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

			//do this only when online/offline actions are performed 
			pmgr.getTransaction().begin();
			try {
				Node local = pmgr.find(Node.class, nodeId);
				if (local != null) {
					local.setState(localNodeState.get());
					local.setHeartBeat(now);
					pmgr.merge(local);
					// TODO check version with previous to detect nodeId conflict 
				} else {
					local = new Node();
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
			NodeStatusImpl status = new NodeStatusImpl(clusterId, nodeId, localNodeState.get());
			BytesMessage message = nodeStatusSession.createBytesMessage();
			Marshaller marshaller = CLUSTER_JAXB_CTX.createMarshaller();
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			marshaller.marshal(status, bos);
			message.writeBytes(bos.toByteArray());
			producer.send(message);

			while (true) {
				try {
					message = (BytesMessage) consumer.receive(0l);
					if (message == null) {
						break;
					}
					byte[] payload = new byte[(int) message.getBodyLength()];
					message.readBytes(payload);
					Unmarshaller umarshaller = CLUSTER_JAXB_CTX.createUnmarshaller();
					status = (NodeStatusImpl) umarshaller.unmarshal(new ByteArrayInputStream(payload));
					currentNodes.add(status);
				} catch (JMSException e) {
					log.log(Level.SEVERE, "", e);
				}
			}

			for (NodeStatusImpl n : nodeStatus.get()) {
				NodeState nState = NodeState.ONLINE.equals(n.getState()) && n.getHeartbeat().compareTo(active) >= 0 ? NodeState.ONLINE : NodeState.OFFLINE;
				currentNodes.add(n);
				if (clusterId.equalsIgnoreCase(n.getClusterId()) && NodeState.ONLINE.equals(nState)) {
					currentOnlineClusterNodes.add(n.getNodeId());
				}
			}

			nodeStatus.set(Collections.unmodifiableSet(currentNodes));
			onlineClusterNodes.set(Collections.unmodifiableSet(currentOnlineClusterNodes));
		} catch (Throwable t) {
			log.log(Level.SEVERE, "", t);
		}
	}
}
