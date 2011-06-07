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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

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

import org.apache.ode.runtime.exec.cluster.xml.ClusterConfig;
import org.apache.ode.spi.exec.Action;
import org.apache.ode.spi.exec.Action.ActionId;
import org.apache.ode.spi.exec.ActionStatus;
import org.apache.ode.spi.exec.PlatformException;
import org.apache.ode.spi.exec.Target;
import org.apache.ode.spi.repo.Repository;
import org.apache.ode.spi.repo.RepositoryException;
import org.w3c.dom.Document;

/**
 * A naive cluster implementation based on database polling. When the JSR 107
 * spec solidifies (http://github.com/jsr107) and a distributed implementation
 * is available this implementation should be migrated to it. A distributed
 * cache with update write throughs and cache listeners for acting on the
 * updates would be a more efficient implementation
 * 
 */
@Singleton
public class Cluster {

	public static final String CLUSTER_CONFIG_MIMETYPE = "application/ode-cluster-config";
	public static final String CLUSTER_CONFIG_NAMESPACE = "http://ode.apache.org/cluster-config";
	public static JAXBContext CLUSTER_CONFIG_JAXB_CTX;
	static {
		try {
			CLUSTER_CONFIG_JAXB_CTX = JAXBContext.newInstance("org.apache.ode.runtime.exec.cluster.xml");
		} catch (JAXBException je) {
			je.printStackTrace();
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
	LocalNode localNode;

	ScheduledExecutorService clusterScheduler;

	@PostConstruct
	public void start() {
		config = loadClusterConfig();
		healthCheck.setConfig(config.getHealthCheck());
		actionPoll.setConfig(config.getActionCheck());

		clusterScheduler = new ScheduledThreadPoolExecutor(2, new ThreadFactory() {

			@Override
			public Thread newThread(Runnable r) {
				return new Thread("ODE-X Cluster Scheduler");
			}
		});
		clusterScheduler.scheduleAtFixedRate(healthCheck, 0, config.getHealthCheck().getFrequency(), TimeUnit.MILLISECONDS);
		clusterScheduler.scheduleAtFixedRate(actionPoll, 0, config.getActionCheck().getFrequency(), TimeUnit.MILLISECONDS);
	}

	@PreDestroy
	public void stop() {
		clusterScheduler.shutdownNow();
	}

	public ActionId execute(Action action, Document actionInfo, Target... targets) throws PlatformException {
		return null;
	}

	public ActionStatus status(ActionId actionId) throws PlatformException {
		return null;
	}

	public void cancel(ActionId actionId) throws PlatformException {
		
	}

	@Qualifier
	@java.lang.annotation.Target({ ElementType.FIELD })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface ClusterId {

	}

	@Qualifier
	@java.lang.annotation.Target({ ElementType.FIELD })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface NodeId {

	}

	ClusterConfig loadClusterConfig() {
		QName configName = new QName(CLUSTER_CONFIG_NAMESPACE, clusterId);
		try {
			repo.read(configName, CLUSTER_CONFIG_MIMETYPE, "1.0", JAXBElement.class);
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
		System.out.println("Unable to load cluster config, using default config");
		try {
			Unmarshaller u = CLUSTER_CONFIG_JAXB_CTX.createUnmarshaller();
			JAXBElement<ClusterConfig> config = (JAXBElement<ClusterConfig>) u.unmarshal(Thread.currentThread().getContextClassLoader()
					.getResourceAsStream("/META-INF/default_cluster.xml"));
			repo.create(configName, CLUSTER_CONFIG_MIMETYPE, "1.0", config);
			return config.getValue();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
