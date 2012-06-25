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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.DataSource;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Qualifier;
import javax.inject.Singleton;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.ode.runtime.exec.cluster.xml.ClusterConfig;
import org.apache.ode.runtime.exec.platform.task.TaskExecutor;
import org.apache.ode.runtime.exec.platform.task.TaskImpl.TaskIdImpl;
import org.apache.ode.spi.exec.Component;
import org.apache.ode.spi.exec.Component.InstructionSet;
import org.apache.ode.spi.exec.Executors;
import org.apache.ode.spi.exec.Node;
import org.apache.ode.spi.exec.Platform.NodeStatus;
import org.apache.ode.spi.exec.Platform.NodeStatus.NodeState;
import org.apache.ode.spi.exec.PlatformException;
import org.apache.ode.spi.exec.Target;
import org.apache.ode.spi.exec.Task;
import org.apache.ode.spi.exec.Task.TaskActionDefinition;
import org.apache.ode.spi.exec.Task.TaskDefinition;
import org.apache.ode.spi.exec.Task.TaskId;
import org.apache.ode.spi.exec.Task.TaskState;
import org.apache.ode.spi.repo.JAXBDataContentHandler;
import org.apache.ode.spi.repo.Repository;
import org.apache.ode.spi.repo.RepositoryException;
import org.w3c.dom.Document;

@Singleton
public class NodeImpl implements Node {

	public static JAXBContext CLUSTER_JAXB_CTX;
	private static final Logger log = Logger.getLogger(NodeImpl.class.getName());

	static {
		try {
			CLUSTER_JAXB_CTX = JAXBContext.newInstance("org.apache.ode.runtime.exec.cluster.xml");
		} catch (JAXBException je) {
			log.log(Level.SEVERE, "", je);
		}
	}

	@PersistenceUnit(unitName = "platform")
	EntityManagerFactory pmgrFactory;

	@Inject
	ClusterConfig config;

	@ClusterId
	String clusterId;

	@NodeId
	String nodeId;

	@Inject
	Repository repo;

	@Inject
	HealthCheck healthCheck;

	@Inject
	MessageHandler messageHandler;

	@Inject
	TaskExecutor taskExec;

	@LocalNodeState
	AtomicReference<NodeState> localNodeState;

	private Map<QName, Component> components = new ConcurrentHashMap<QName, Component>();
	private Map<QName, InstructionSet> instructions = new ConcurrentHashMap<QName, InstructionSet>();
	private Map<QName, TaskDefinition> tasks = new ConcurrentHashMap<QName, TaskDefinition>();
	private Map<QName, TaskActionDefinition> actions = new ConcurrentHashMap<QName, TaskActionDefinition>();

	private QName architecture;

	@Inject
	Executors executors;

	@Singleton
	public static class LocalNodeStateProvider implements Provider<AtomicReference<NodeState>> {
		AtomicReference<NodeState> localNodeState = new AtomicReference<NodeState>();

		@Override
		public AtomicReference<NodeState> get() {
			return localNodeState;
		}

	}

	@PostConstruct
	public void init() {
		log.fine("Initializing Node");
		taskExec.configure(this);
		localNodeState.set(NodeState.OFFLINE);

		// Prime the health check to make sure it runs at least once before
		// continuing startup
		// healthCheck.run();
		ScheduledExecutorService clusterScheduler;
		try {
			clusterScheduler = executors.initClusterTaskScheduler();
			clusterScheduler.scheduleAtFixedRate(healthCheck, 0, config.getHealthCheck().getFrequency(), TimeUnit.MILLISECONDS);
			clusterScheduler.scheduleAtFixedRate(taskExec, 0, config.getTaskCheck().getFrequency(), TimeUnit.MILLISECONDS);
			clusterScheduler.scheduleAtFixedRate(messageHandler, 0, config.getMessageCheck().getFrequency(), TimeUnit.MILLISECONDS);
		} catch (PlatformException pe) {
			log.log(Level.SEVERE, "", pe);
		}
		log.fine("Cluster Initialized");
	}

	@PreDestroy
	public void destroy() {
		try {
			executors.destroyClusterTaskScheduler();
		} catch (PlatformException pe) {
			log.log(Level.SEVERE, "", pe);
		}

	}

	@Override
	public QName architecture() {
		return architecture;
	}

	public void setArchitecture(QName architecture) {
		this.architecture = architecture;
	}

	@Override
	public void registerComponent(Component component) {
		components.put(component.name(), component);
		for (InstructionSet is : component.instructionSets()) {
			instructions.put(is.getName(), is);
		}
		for (TaskDefinition td : component.tasks()) {
			tasks.put(td.task(), td);
		}
		for (TaskActionDefinition tad : component.actions()) {
			actions.put(tad.action(), tad);
		}

	}

	@Override
	public Map<QName, InstructionSet> getInstructionSets() {
		return instructions;
	}

	@Override
	public Map<QName, TaskDefinition> getTaskDefinitions() {
		return tasks;
	}
	
	@Override
	public Map<QName, TaskActionDefinition> getTaskActionDefinitions() {
		return actions;
	}

	@Override
	public void online() throws PlatformException {
		taskExec.setupTasks(components.values());
		taskExec.online();
		localNodeState.set(NodeState.ONLINE);
		healthCheck.run();
	}

	@Override
	public void offline() throws PlatformException {
		taskExec.offline();
		localNodeState.set(NodeState.OFFLINE);
		healthCheck.run();
	}

	public void executeSync(QName task, Document taskInput, Target... targets) throws PlatformException {
		Future<TaskState> state = taskExec.submitTask(null, null, task, taskInput, targets).state;
		try {
			state.get();
		} catch (InterruptedException | ExecutionException e) {
			throw new PlatformException(e);
		}
	}

	public TaskIdImpl executeAsync(QName task, Document taskInput, Target... targets) throws PlatformException {
		return taskExec.submitTask(null, null, task, taskInput, targets).id;
	}

	public Task status(TaskId taskId) throws PlatformException {
		//return actionExec.status((ActionIdImpl) actionId);
		return null;
	}

	public void cancel(TaskId taskId) throws PlatformException {
		//actionExec.cancel((ActionIdImpl) actionId);
	}

	/**
	 * Available targets: LOCAL, CLUSTER, NODE
	 * 
	 * @param targets
	 * @return
	 * @throws PlatformException
	 */
	public String getTarget(Target[] targets) throws PlatformException {
		/*
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
		*/
		return null;
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

	@Qualifier
	@java.lang.annotation.Target({ ElementType.FIELD, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface LocalNodeState {

	}

	@Qualifier
	@java.lang.annotation.Target({ ElementType.FIELD, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface NodeCheck {

	}

	@Qualifier
	@java.lang.annotation.Target({ ElementType.FIELD, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface TaskCheck {

	}

	@Qualifier
	@java.lang.annotation.Target({ ElementType.FIELD, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface MessageCheck {

	}

	@Singleton
	public static class ClusterConfigProvider implements Provider<ClusterConfig> {
		@ClusterId
		private String clusterId;

		@NodeId
		private String nodeId;

		@Inject
		Repository repo;

		private ClusterConfig config;

		public ClusterConfig get() {
			return config;
		}

		@PostConstruct
		public void init() throws Exception {
			log.fine("Initializing ClusterConfigProvider");
			repo.registerNamespace(CLUSTER_NAMESPACE, CLUSTER_MIMETYPE);
			repo.registerHandler(CLUSTER_MIMETYPE, new JAXBDataContentHandler(CLUSTER_JAXB_CTX) {
				@Override
				public QName getDefaultQName(DataSource dataSource) {
					QName defaultName = null;
					try {
						InputStream is = dataSource.getInputStream();
						XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(is);
						reader.nextTag();
						String tns = CLUSTER_NAMESPACE;
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

			QName configName = new QName(CLUSTER_NAMESPACE, clusterId);
			try {
				JAXBElement<ClusterConfig> config = repo.read(configName, CLUSTER_MIMETYPE, "1.0", JAXBElement.class);
				this.config = config.getValue();
				return;
			} catch (RepositoryException e) {

			}
			log.log(Level.WARNING, "Unable to load cluster config, using default config");
			try {
				Unmarshaller u = CLUSTER_JAXB_CTX.createUnmarshaller();
				JAXBElement<ClusterConfig> config = (JAXBElement<ClusterConfig>) u.unmarshal(getClass().getResourceAsStream("/META-INF/default_cluster.xml"));
				repo.create(configName, CLUSTER_MIMETYPE, "1.0", config);
				this.config = config.getValue();
			} catch (Exception e) {
				log.log(Level.SEVERE, "", e);
				throw e;
			}
			if (!config.getNodes().isAutoDiscovery() && !config.getNodes().getNode().contains(nodeId)) {
				log.log(Level.SEVERE, "Node auth discovery disabled and nodeId {0} is undeclared, aborting", nodeId);
				throw new Exception(String.format("Undefined node %s in cluster %s", nodeId, clusterId));
			}
			if (config.getHealthCheck() == null) {
				config.setHealthCheck(new org.apache.ode.runtime.exec.cluster.xml.HealthCheck());
			}
			if (config.getTaskCheck() == null) {
				config.setTaskCheck(new org.apache.ode.runtime.exec.cluster.xml.TaskCheck());
			}
			if (config.getMessageCheck() == null) {
				config.setMessageCheck(new org.apache.ode.runtime.exec.cluster.xml.MessageCheck());
			}

		}
	}

}
