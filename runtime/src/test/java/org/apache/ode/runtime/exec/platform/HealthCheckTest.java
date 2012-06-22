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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

import java.io.ByteArrayInputStream;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Provider;
import javax.jms.Queue;
import javax.jms.Topic;
import javax.jms.TopicConnectionFactory;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import org.apache.ode.runtime.exec.cluster.xml.ClusterConfig;
import org.apache.ode.runtime.exec.platform.JMSUtil.QueueImpl;
import org.apache.ode.runtime.exec.platform.JMSUtil.TopicConnectionFactoryImpl;
import org.apache.ode.runtime.exec.platform.JMSUtil.TopicImpl;
import org.apache.ode.runtime.exec.platform.NodeImpl.ClusterConfigProvider;
import org.apache.ode.runtime.exec.platform.NodeImpl.ClusterId;
import org.apache.ode.runtime.exec.platform.NodeImpl.LocalNodeState;
import org.apache.ode.runtime.exec.platform.NodeImpl.LocalNodeStateProvider;
import org.apache.ode.runtime.exec.platform.NodeImpl.NodeId;
import org.apache.ode.runtime.exec.platform.NodeModule.NodeTypeListener;
import org.apache.ode.spi.exec.Node;
import org.apache.ode.spi.exec.Platform.NodeStatus.NodeState;
import org.apache.ode.spi.repo.DataContentHandler;
import org.apache.ode.spi.repo.Repository;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.Key;
import com.google.inject.matcher.Matchers;
import com.mycila.inject.jsr250.Jsr250;
import com.mycila.inject.jsr250.Jsr250Injector;

public class HealthCheckTest {
	private static final Logger log = Logger.getLogger(HealthCheckTest.class.getName());
	private static Jsr250Injector injector1;
	private static Jsr250Injector injector2;

	private static Topic healthCheckTopic = new TopicImpl(Node.NODE_MQ_NAME_HEALTHCHECK);
	private static Queue taskQueue = new QueueImpl(Node.NODE_MQ_NAME_TASK);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		injector1 = Jsr250.createInjector(new JPAModule(), new RepoModule(), new HealthCheckTestModule("hcluster", "node1"));
		loadTestClusterConfig(injector1, "hcluster");
		injector2 = Jsr250.createInjector(new JPAModule(), new RepoModule(), new HealthCheckTestModule("hcluster", "node2"));
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		injector1.destroy();
		injector2.destroy();
	}

	public static class HealthCheckTestModule extends JMSModule {
		String clusterId;
		String nodeId;

		public HealthCheckTestModule(String clusterId, String nodeId) {
			this.clusterId = clusterId;
			this.nodeId = nodeId;
		}

		@Override
		protected void configure() {
			super.configure();
			bindListener(Matchers.any(), new NodeTypeListener());
			bind(AtomicReference.class).annotatedWith(LocalNodeState.class).toProvider(LocalNodeStateProvider.class);
			bind(ClusterConfig.class).toProvider(ClusterConfigProvider.class);
			bind(HealthCheck.class);
			bindConstant().annotatedWith(NodeId.class).to(nodeId);
			bindConstant().annotatedWith(ClusterId.class).to(clusterId);
		}

		@Override
		protected Class<? extends Provider<Topic>> getHealthCheckTopic() {
			class HealthCheckTopic implements Provider<Topic> {
				

				@Override
				public Topic get() {
					return healthCheckTopic;
				}

			}
			return HealthCheckTopic.class;
		}
		@Override
		protected Class<? extends Provider<Queue>> getTaskQueue() {
			class TaskQueue implements Provider<Queue> {
			
				@Override
				public Queue get() {
					return taskQueue;
				}

			}
			return TaskQueue.class;
		}
	}

	public static void loadTestClusterConfig(Jsr250Injector injector, String clusterId) {
		Repository repo = injector.getInstance(Repository.class);
		try {
			byte[] content = DataContentHandler.readStream(TaskTest.class.getResourceAsStream("/META-INF/test_cluster.xml"));
			Unmarshaller u = NodeImpl.CLUSTER_JAXB_CTX.createUnmarshaller();
			JAXBElement<ClusterConfig> config = (JAXBElement<ClusterConfig>) u.unmarshal(new ByteArrayInputStream(content));
			ClusterConfig clusterConfig = config.getValue();
			repo.create(new QName(Node.CLUSTER_NAMESPACE, clusterId), Node.CLUSTER_MIMETYPE, "1.0", content);
		} catch (Exception e) {
			log.log(Level.SEVERE, "", e);
		}
	}

	@Test
	public void healthCheckTest() throws Exception {
		AtomicReference<NodeState> localNodeState1 = injector1.getInstance(Key.get(AtomicReference.class,LocalNodeState.class));
		assertNotNull(localNodeState1);
		localNodeState1.set(NodeState.OFFLINE);
		AtomicReference<NodeState> localNodeState2 = injector2.getInstance(Key.get(AtomicReference.class,LocalNodeState.class));
		assertNotNull(localNodeState2);
		localNodeState2.set(NodeState.OFFLINE);

		HealthCheck check1 = injector1.getInstance(HealthCheck.class);
		assertNotNull(check1);
		HealthCheck check2 = injector2.getInstance(HealthCheck.class);
		assertNotSame(check1, check2);

		check1.run();
		assertEquals(1, check1.availableNodes().size());
		assertEquals(0, check1.onlineClusterNodes().size());

		check2.run();
		assertEquals(2, check2.availableNodes().size());
		assertEquals(0, check2.onlineClusterNodes().size());

		localNodeState1.set(NodeState.ONLINE);
		check1.run();
		assertEquals(1, check1.onlineClusterNodes().size());
		localNodeState2.set(NodeState.ONLINE);
		check2.run();
		assertEquals(2, check2.onlineClusterNodes().size());

	}
}
