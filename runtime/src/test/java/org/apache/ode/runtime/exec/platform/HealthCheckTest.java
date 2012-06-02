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

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import org.apache.ode.runtime.exec.platform.NodeImpl.ClusterId;
import org.apache.ode.runtime.exec.platform.NodeImpl.NodeId;
import org.apache.ode.spi.exec.Platform.NodeStatus.NodeState;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.mycila.inject.jsr250.Jsr250;
import com.mycila.inject.jsr250.Jsr250Injector;

public class HealthCheckTest {
	private static final Logger log = Logger.getLogger(HealthCheckTest.class.getName());
	private static Jsr250Injector injector;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		injector = Jsr250.createInjector(new JPAModule(), new RepoModule(), new JMSModule(), new HealthCheckTestModule("cluster1", "node1"));
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		injector.destroy();
	}

	public static class HealthCheckTestModule extends AbstractModule {
		String clusterId;
		String nodeId;

		public HealthCheckTestModule(String clusterId, String nodeId) {
			this.clusterId = clusterId;
			this.nodeId = nodeId;
		}

		@Override
		protected void configure() {
			bind(HealthCheck.class);
			bind(String.class).annotatedWith(NodeId.class).toInstance(nodeId);
			bind(String.class).annotatedWith(ClusterId.class).toInstance(clusterId);
		}
	}

	@Test
	public void programTest() throws Exception {
		AtomicReference<NodeState> localNodeState1 = new AtomicReference<NodeState>();
		localNodeState1.set(NodeState.OFFLINE);
		AtomicReference<NodeState> localNodeState2 = new AtomicReference<NodeState>();
		localNodeState2.set(NodeState.OFFLINE);

		org.apache.ode.runtime.exec.cluster.xml.HealthCheck config = new org.apache.ode.runtime.exec.cluster.xml.HealthCheck();
		config.setFrequency(5000l);
		config.setInactiveTimeout(10000l);
		config.setNodeCleanup(DatatypeFactory.newInstance().newDuration("P10D"));

		HealthCheck check1 = injector.getInstance(HealthCheck.class);
		assertNotNull(check1);
		check1.config("cluster1", "node1", localNodeState1, config);
		HealthCheck check2 = injector.getInstance(HealthCheck.class);
		assertNotSame(check1, check2);
		check2.config("cluster1", "node2", localNodeState2, config);
		
		
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
