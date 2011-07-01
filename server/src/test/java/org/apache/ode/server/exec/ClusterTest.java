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
package org.apache.ode.server.exec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.util.Calendar;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.util.AnnotationLiteral;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import org.apache.ode.runtime.exec.cluster.xml.ClusterConfig;
import org.apache.ode.runtime.exec.platform.Cluster;
import org.apache.ode.server.cdi.JPAHandler;
import org.apache.ode.server.cdi.RepoHandler;
import org.apache.ode.server.cdi.RuntimeHandler;
import org.apache.ode.server.cdi.StaticHandler;
import org.apache.ode.server.exec.ClusterComponent.LocalTestAction;
import org.apache.ode.server.exec.ClusterComponent.RemoteTestAction;
import org.apache.ode.spi.exec.Action;
import org.apache.ode.spi.exec.Action.TaskType;
import org.apache.ode.spi.exec.ActionTask.ActionId;
import org.apache.ode.spi.exec.ActionTask.ActionState;
import org.apache.ode.spi.exec.ActionTask.ActionStatus;
import org.apache.ode.spi.exec.NodeStatus;
import org.apache.ode.spi.exec.NodeStatus.NodeState;
import org.apache.ode.spi.exec.Target;
import org.apache.ode.spi.repo.DataContentHandler;
import org.apache.ode.spi.repo.Repository;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

public class ClusterTest {
	private static Weld weld;
	protected static WeldContainer container;
	protected static Cluster cluster;
	protected static ClusterAssistant assistant;
	protected static ClusterConfig clusterConfig;
	protected static ClusterComponent clusterComponent;

	final static String clusterId = "test-cluster";
	final static String nodeId = "testNode1";
	final static String testNodeId = "testNode2";

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		StaticHandler.clear();
		StaticHandler.addDelegate(new JPAHandler());
		StaticHandler.addDelegate(new RepoHandler());
		StaticHandler.addDelegate(new RuntimeHandler() {

			@Override
			public void beforeBeanDiscovery(BeforeBeanDiscovery bbd, BeanManager bm) {
				bbd.addAnnotatedType(bm.createAnnotatedType(ClusterComponent.class));
				bbd.addAnnotatedType(bm.createAnnotatedType(ClusterAssistant.class));
				super.beforeBeanDiscovery(bbd, bm);
			}

			@Override
			public void afterDeploymentValidation(AfterDeploymentValidation adv, BeanManager bm) {
				this.manage(ClusterComponent.class);
				this.manage(ClusterAssistant.class);
				// Load up the test cluster config
				Set<Bean<?>> beans = bm.getBeans(Repository.class, new AnnotationLiteral<Any>() {
				});
				if (beans.size() > 0) {
					Bean<Repository> repoBean = (Bean<Repository>) beans.iterator().next();
					CreationalContext<Repository> ctx = bm.createCreationalContext(repoBean);
					Repository repo = (Repository) bm.getReference(repoBean, Repository.class, ctx);
					try {
						byte[] content = DataContentHandler.readStream(getClass().getResourceAsStream("/META-INF/test_cluster.xml"));
						Unmarshaller u = Cluster.CLUSTER_JAXB_CTX.createUnmarshaller();
						JAXBElement<ClusterConfig> config = (JAXBElement<ClusterConfig>) u.unmarshal(new ByteArrayInputStream(content));
						clusterConfig = config.getValue();
						repo.create(new QName(Cluster.CLUSTER_CONFIG_NAMESPACE, clusterId), Cluster.CLUSTER_CONFIG_MIMETYPE, "1.0", content);
					} catch (Exception e) {
						e.printStackTrace();
					}
					repoBean.destroy(repo, ctx);
				} else {
					System.out.println("Can't find class " + Repository.class);
				}
				super.afterDeploymentValidation(adv, bm);
				cluster = (Cluster) getInstance(Cluster.class);
				assistant = (ClusterAssistant) getInstance(ClusterAssistant.class);
				assistant.setup(testNodeId, clusterId, clusterConfig);
				clusterComponent = (ClusterComponent) getInstance(ClusterComponent.class);
			}

			@Override
			public void beforeShutdown(BeforeShutdown adv, BeanManager bm) {
				super.stop();
			}
		});
		weld = new Weld();
		container = weld.initialize();

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		try {
			weld.shutdown();
		} catch (NullPointerException e) {
		}
	}

	@Test
	public void testNodeAutoDiscovery() throws Exception {
		assertNotNull(cluster);
		assertNotNull(assistant);
		cluster.offline();
		assistant.offline();
		nodeStatus(assistant.availableNodes(), false, false);
		assistant.online();
		nodeStatus(assistant.availableNodes(), false, true);
		cluster.online();
		assistant.healthCheck();// detect other cluster heartbeat
		nodeStatus(assistant.availableNodes(), true, true);
		cluster.offline();
		assistant.offline();
		nodeStatus(assistant.availableNodes(), false, false);
	}

	void nodeStatus(Set<NodeStatus> status, boolean node1, boolean node2) {
		assertEquals(2, status.size());
		for (NodeStatus s : status) {
			if (nodeId.equals(s.nodeId())) {
				if (node1) {
					assertTrue(NodeState.ONLINE.equals(s.state()));
				} else {
					assertTrue(NodeState.OFFLINE.equals(s.state()));
				}
			} else if (testNodeId.equals(s.nodeId())) {
				if (node2) {
					assertTrue(NodeState.ONLINE.equals(s.state()));
				} else {
					assertTrue(NodeState.OFFLINE.equals(s.state()));
				}
			} else {
				fail("Unknown node id " + s.nodeId());
			}
		}
	}

	@Test
	public void testHeartbeat() throws Exception {
		assertNotNull(assistant);
		Calendar first = assistant.getHeartBeat(nodeId);
		boolean passed = false;
		for (int i = 0; i < 25; i++) {
			Calendar second = assistant.getHeartBeat(nodeId);
			if (second.compareTo(first) > 0) {
				passed = true;
				break;
			}
			Thread.sleep(100);
		}
		assertTrue(passed);
	}

	@Test
	public void testLocalAction() throws Exception {
		assertNotNull(cluster);
		assertNotNull(clusterComponent);

		cluster.offline();
		clusterComponent.actions().clear();
		clusterComponent.actions().add(new Action(ClusterComponent.TEST_LOCAL_ACTION, TaskType.ACTION, LocalTestAction.getProvider()));
		cluster.online();
		ActionId id = cluster.execute(ClusterComponent.TEST_LOCAL_ACTION, ClusterComponent.testActionDoc("localTest"), Target.LOCAL);
		assertNotNull(id);

		LocalTestAction.notify(2, TimeUnit.SECONDS);
		ActionStatus status = cluster.status(id);
		assertNotNull(status);
		assertEquals(ActionState.START, status.state());
		LocalTestAction.notify(2, TimeUnit.SECONDS);

		LocalTestAction.notify(2, TimeUnit.SECONDS);
		status = cluster.status(id);
		assertNotNull(status);
		assertEquals(ActionState.EXECUTING, status.state());
		LocalTestAction.notify(2, TimeUnit.SECONDS);

		LocalTestAction.notify(2, TimeUnit.SECONDS);
		status = cluster.status(id);
		assertNotNull(status);
		assertEquals(ActionState.FINISH, status.state());
		LocalTestAction.notify(2, TimeUnit.SECONDS);

		boolean passed = false;
		for (int i = 0; i < 25; i++) {
			status = cluster.status(id);
			assertNotNull(status);
			if (ActionState.COMPLETED.equals(status.state())) {
				passed = true;
				break;
			}
			Thread.sleep(100);
		}
		assertTrue(passed);
	}

	
	@Test
	public void testRemoteAction() throws Exception {
		assertNotNull(cluster);
		assertNotNull(clusterComponent);
		assertNotNull(assistant);

		cluster.offline();
		assistant.offline();
		clusterComponent.actions().clear();
		clusterComponent.actions().add(new Action(ClusterComponent.TEST_REMOTE_ACTION, TaskType.ACTION, RemoteTestAction.getProvider()));
		cluster.online();
		assistant.online();
		ActionId id = assistant.executeRemoteAction( ClusterComponent.testActionDoc("remoteTest"), nodeId);
		assertNotNull(id);


		boolean passed = false;
		Document result=null;
		for (int i = 0; i < 50; i++) {
			ActionStatus status = cluster.status(id);
			assertNotNull(status);
			if (ActionState.COMPLETED.equals(status.state())) {
				result=status.result();
				passed = true;
				break;
			}
			Thread.sleep(100);
		}
		assertTrue(passed);
		assertNotNull(result);
		assertEquals("remoteTest start run finish",result.getDocumentElement().getTextContent());
	}

	@Test
	public void testMasterSlaveAction() throws Exception {
		assertNotNull(cluster);
		/*
		 * ActionId id =
		 * cluster.execute(ClusterComponent.TEST_ACTION.getQName(),
		 * ClusterComponent.testActionInput("localTest"), Target.ALL);
		 * assertNotNull(id); ActionStatus status = cluster.status(id);
		 * assertNotNull(status);
		 * assertEquals(Status.EXECUTING,status.status());
		 */
	}

	@Test
	public void testStatus() throws Exception {

	}

	@Test
	public void testCancel() throws Exception {

	}

}
