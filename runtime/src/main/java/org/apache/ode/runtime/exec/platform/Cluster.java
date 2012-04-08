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

import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.DataSource;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.inject.Singleton;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.ode.runtime.exec.cluster.xml.ClusterConfig;
import org.apache.ode.spi.exec.Action.TaskType;
import org.apache.ode.spi.exec.ActionTask.ActionId;
import org.apache.ode.spi.exec.ActionTask.ActionStatus;
import org.apache.ode.spi.exec.Component;
import org.apache.ode.spi.exec.Executors;
import org.apache.ode.spi.exec.NodeStatus;
import org.apache.ode.spi.exec.NodeStatus.NodeState;
import org.apache.ode.spi.exec.PlatformException;
import org.apache.ode.spi.exec.Target;
import org.apache.ode.spi.exec.Target.TargetType;
import org.apache.ode.spi.repo.JAXBDataContentHandler;
import org.apache.ode.spi.repo.Repository;
import org.apache.ode.spi.repo.RepositoryException;
import org.w3c.dom.Document;

/**
 * A naive cluster implementation based on database polling. When the JSR 107 spec solidifies (http://github.com/jsr107) and a distributed implementation is
 * available this implementation should be migrated to it. A distributed cache with update write throughs and cache listeners for acting on the updates would be
 * a more efficient implementation
 * 
 */
@Singleton
public class Cluster {

	public static final String CLUSTER_CONFIG_MIMETYPE = "application/ode-cluster-config";
	public static final String CLUSTER_CONFIG_NAMESPACE = "http://ode.apache.org/cluster-config";
	public static JAXBContext CLUSTER_JAXB_CTX;
	private static final Logger log = Logger.getLogger(Cluster.class.getName());

	static {
		try {
			CLUSTER_JAXB_CTX = JAXBContext.newInstance("org.apache.ode.runtime.exec.cluster.xml");
		} catch (JAXBException je) {
			log.log(Level.SEVERE, "", je);
		}
	}
	@ClusterId
	String clusterId;

	@NodeId
	String nodeId;

	@Inject
	Repository repo;

	ClusterConfig config;

	@Inject
	HealthCheck healthCheck;

	@Inject
	ActionPoll actionPoll;

	@Inject
	ActionExecutor actionExec;

	Set<Component> components = Collections.synchronizedSet(new HashSet<Component>());

	AtomicReference<NodeState> localNodeState = new AtomicReference<NodeState>();

	@Inject
	Executors executors;

	@PostConstruct
	public void init() {
		log.fine("Initializing Cluster");
		repo.registerNamespace(CLUSTER_CONFIG_NAMESPACE, CLUSTER_CONFIG_MIMETYPE);
		repo.registerHandler(CLUSTER_CONFIG_MIMETYPE, new JAXBDataContentHandler(CLUSTER_JAXB_CTX) {
			@Override
			public QName getDefaultQName(DataSource dataSource) {
				QName defaultName = null;
				try {
					InputStream is = dataSource.getInputStream();
					XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(is);
					reader.nextTag();
					String tns = CLUSTER_CONFIG_NAMESPACE;
					String name = reader.getAttributeValue(null, "name");
					reader.close();
					if (name != null) {
						defaultName = new QName(tns, name);
					}
					return defaultName;
				} catch (Exception e) {
					return null;
				}
			}

		});

		config = loadClusterConfig();
		if (!config.getNodes().isAutoDiscovery() && !config.getNodes().getNode().contains(nodeId)) {
			log.log(Level.SEVERE, "Node auth discovery disabled and nodeId {0} is undeclared, aborting", nodeId);
			return;
		}
		localNodeState.set(NodeState.OFFLINE);
		org.apache.ode.runtime.exec.cluster.xml.HealthCheck healthCheckConfig = config.getHealthCheck() != null ? config.getHealthCheck()
				: new org.apache.ode.runtime.exec.cluster.xml.HealthCheck();
		org.apache.ode.runtime.exec.cluster.xml.ActionCheck actionCheckConfig = config.getActionCheck() != null ? config.getActionCheck()
				: new org.apache.ode.runtime.exec.cluster.xml.ActionCheck();
		healthCheck.init(clusterId, nodeId, localNodeState, healthCheckConfig);
		actionExec.init(nodeId);
		actionPoll.init(clusterId, nodeId, localNodeState, actionExec, actionCheckConfig);

		// Prime the health check to make sure it runs at least once before
		// continuing startup
		// healthCheck.run();
		ScheduledExecutorService clusterScheduler;
		try {
			clusterScheduler = executors.initClusterScheduler();
			clusterScheduler.scheduleAtFixedRate(healthCheck, 0, healthCheckConfig.getFrequency(), TimeUnit.MILLISECONDS);
			clusterScheduler.scheduleAtFixedRate(actionPoll, 0, actionCheckConfig.getFrequency(), TimeUnit.MILLISECONDS);
		} catch (PlatformException pe) {
			log.log(Level.SEVERE, "", pe);
		}
		log.fine("Cluster Initialized");
	}

	@PreDestroy
	public void destroy() {
		try {
			executors.destroyClusterScheduler();
		} catch (PlatformException pe) {
			log.log(Level.SEVERE, "", pe);
		}
		
	}

	public void online() throws PlatformException {
		actionExec.setupActions(components);
		actionExec.online();
		localNodeState.set(NodeState.ONLINE);
		healthCheck.run();
	}

	public void offline() throws PlatformException {
		actionExec.offline();
		localNodeState.set(NodeState.OFFLINE);
		healthCheck.run();
	}

	public void addComponent(Component component) {
		this.components.add(component);
	}

	public ActionId execute(QName action, Document actionInput, Target... targets) throws PlatformException {
		TaskType type = actionExec.actionType(action);

		if (TaskType.ACTION.equals(type)) {
			String target = getTarget(targets);
			return actionExec.executeAction(action, actionInput, target);
		} else if (TaskType.MASTER.equals(type)) {
			Set<String> slaveTargets = getMasterTargets(targets);
			return actionExec.executeMasterAction(action, actionInput, slaveTargets);
		} else {
			throw new PlatformException("Unsupported ActionTask type");
		}

	}

	public ActionStatus status(ActionId actionId) throws PlatformException {
		return actionExec.status((ActionIdImpl) actionId);
	}

	public void cancel(ActionId actionId) throws PlatformException {
		actionExec.cancel((ActionIdImpl) actionId);
	}

	/**
	 * Available targets: LOCAL, CLUSTER, NODE
	 * 
	 * @param targets
	 * @return
	 * @throws PlatformException
	 */
	public String getTarget(Target[] targets) throws PlatformException {

		if (targets == null) {
			targets = new Target[] { new Target(null, TargetType.LOCAL) };
		}

		if (targets.length > 1) {
			throw new PlatformException("Actions may only have a single target");
		}

		if (TargetType.ALL.equals(targets[0].getTargetType())) {
			throw new PlatformException("ALL target not support for ActionTask type Action");
		}
		if (TargetType.LOCAL.equals(targets[0].getTargetType())) {
			return nodeId;
		}

		Set<String> canidates = new HashSet();
		for (NodeStatus n : healthCheck.availableNodes()) {
			if (NodeState.ONLINE.equals(n.state())) {
				for (Target t : targets) {
					switch (t.getTargetType()) {
					case CLUSTER:
						if (n.clusterId().equals(t.getName())) {
							canidates.add(n.nodeId());
						}
						break;
					case NODE:
						if (n.nodeId().equals(t.getName())) {
							canidates.add(n.nodeId());
						}
						break;
					}
				}
			}
		}
		if (canidates.size() == 0) {
			throw new PlatformException(String.format("Invalid target %s %s", targets[0].getTargetType(), targets[0].getName()));
		}
		if (canidates.contains(nodeId)) {
			return nodeId;
		}

		return canidates.iterator().next();
	}

	public Set<String> getMasterTargets(Target[] targets) throws PlatformException {
		if (targets == null) {
			targets = new Target[] { new Target(null, TargetType.ALL) };
		}
		Set<String> actionTarget = new HashSet<String>();
		// Master/Slave Action
		for (NodeStatus n : healthCheck.availableNodes()) {
			if (NodeState.ONLINE.equals(n.state())) {
				for (Target t : targets) {
					switch (t.getTargetType()) {
					case ALL:
						actionTarget.add(n.nodeId());
						break;
					case CLUSTER:
						if (n.clusterId().equals(t.getName())) {
							actionTarget.add(n.nodeId());
						}
						break;
					case NODE:
						if (n.nodeId().equals(t.getName())) {
							actionTarget.add(n.nodeId());
						}
						break;
					case LOCAL:
						actionTarget.add(nodeId);
						break;
					}
				}
			}
		}
		return actionTarget;
	}

	public Set<NodeStatus> status() {
		return healthCheck.availableNodes();
	}

	@Qualifier
	@java.lang.annotation.Target({ ElementType.FIELD, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface ClusterId {

	}

	@Qualifier
	@java.lang.annotation.Target({ ElementType.FIELD, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface NodeId {

	}

	ClusterConfig loadClusterConfig() {
		QName configName = new QName(CLUSTER_CONFIG_NAMESPACE, clusterId);
		try {
			JAXBElement<ClusterConfig> config = repo.read(configName, CLUSTER_CONFIG_MIMETYPE, "1.0", JAXBElement.class);
			return config.getValue();
		} catch (RepositoryException e) {

		}
		log.log(Level.WARNING, "Unable to load cluster config, using default config");
		try {
			Unmarshaller u = CLUSTER_JAXB_CTX.createUnmarshaller();
			JAXBElement<ClusterConfig> config = (JAXBElement<ClusterConfig>) u.unmarshal(getClass().getResourceAsStream("/META-INF/default_cluster.xml"));
			repo.create(configName, CLUSTER_CONFIG_MIMETYPE, "1.0", config);
			return config.getValue();
		} catch (Exception e) {
			log.log(Level.SEVERE, "", e);
		}
		return null;
	}

}
