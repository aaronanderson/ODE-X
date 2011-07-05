/*
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

import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.xml.datatype.Duration;

import org.apache.ode.spi.exec.NodeStatus;
import org.apache.ode.spi.exec.NodeStatus.NodeState;

public class HealthCheck implements Runnable {

	@PersistenceContext(unitName = "platform")
	private EntityManager pmgr;
	private AtomicReference<Set<NodeStatus>> nodeStatus = new AtomicReference<Set<NodeStatus>>();
	private AtomicReference<Set<String>> onlineClusterNodes = new AtomicReference<Set<String>>();
	private org.apache.ode.runtime.exec.cluster.xml.HealthCheck config;
	private String clusterId;
	private String nodeId;
	AtomicReference<NodeState> localNodeState;
	
	private static final Logger log = Logger.getLogger(HealthCheck.class.getName());

	@Override
	public synchronized void run() {
		try {// stop for nothing
			Calendar now = GregorianCalendar.getInstance();
			// Update the node status
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

			// Read in the existing node status
			Calendar lifeTime = (Calendar) now.clone();
			Duration expiration = config.getNodeCleanup().negate();
			expiration.addTo(lifeTime);
			Calendar active = (Calendar) now.clone();
			active.add(Calendar.MILLISECOND, (int) -config.getInactiveTimeout());
			List<Node> nodes = null;
			try {
				Query hcheckQ = pmgr.createNamedQuery("healthCheck");
				hcheckQ.setParameter("lifetime", lifeTime);
				nodes = (List<Node>) hcheckQ.getResultList();
			} catch (PersistenceException pe) {
				log.log(Level.SEVERE, "", pe);
			}
			HashSet<NodeStatus> currentNodes = new HashSet<NodeStatus>();
			HashSet<String> currentOnlineClusterNodes = new HashSet<String>();
			for (Node n : nodes) {
				NodeState nState = NodeState.ONLINE.equals(n.getState()) && n.getHeartBeat().compareTo(active) >= 0 ? NodeState.ONLINE : NodeState.OFFLINE;
				NodeStatusImpl status = new NodeStatusImpl(n.getClusterId(), n.getNodeId(), nState);
				currentNodes.add(status);
				if (clusterId.equalsIgnoreCase(n.getClusterId()) && NodeState.ONLINE.equals(nState)) {
					currentOnlineClusterNodes.add(n.getNodeId());
				}
			}
			pmgr.clear();

			nodeStatus.set(Collections.unmodifiableSet(currentNodes));
			onlineClusterNodes.set(Collections.unmodifiableSet(currentOnlineClusterNodes));
		} catch (Throwable t) {
			log.log(Level.SEVERE, "", t);
		}
	}

	public void init(String clusterId, String nodeId, AtomicReference<NodeState> localNodeStatus, org.apache.ode.runtime.exec.cluster.xml.HealthCheck config) {
		this.clusterId = clusterId;
		this.nodeId = nodeId;
		this.localNodeState = localNodeStatus;
		this.config = config;
		deleteStaleNodes();
	}

	public Set<NodeStatus> availableNodes() {
		return nodeStatus.get();
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

	public static class NodeStatusImpl implements NodeStatus {

		final String clusterId;
		final String nodeId;
		final NodeState state;

		public NodeStatusImpl(String clusterId, String nodeId, NodeState state) {
			this.clusterId = clusterId;
			this.nodeId = nodeId;
			this.state = state;
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
			return state;
		}

	}

}
