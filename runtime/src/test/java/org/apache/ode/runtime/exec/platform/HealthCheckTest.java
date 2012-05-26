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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

import java.util.logging.Logger;

import org.apache.ode.runtime.exec.platform.Cluster.ClusterId;
import org.apache.ode.runtime.exec.platform.Cluster.NodeId;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.mycila.inject.jsr250.Jsr250;
import com.mycila.inject.jsr250.Jsr250Injector;

public class HealthCheckTest {
	private static final Logger log = Logger.getLogger(HealthCheckTest.class.getName());
	private static Jsr250Injector injector1;
	private static Jsr250Injector injector2;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		injector1 = Jsr250.createInjector(new JPAModule(),new RepoModule(),new JMSModule(), new HealthCheckTestModule("cluster1","node1"));
		injector2 = Jsr250.createInjector(new JPAModule(),new RepoModule(),new JMSModule(), new HealthCheckTestModule("cluster1","node2"));
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		injector1.destroy();
		injector2.destroy();
	}

	public static class HealthCheckTestModule extends AbstractModule {
		String clusterId;
		String nodeId;
		
		public HealthCheckTestModule(String clusterId, String nodeId){
			this.clusterId=clusterId;
			this.nodeId=nodeId;
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
		HealthCheck check1 = injector1.getInstance(HealthCheck.class);
		assertNotNull(check1);
		
		HealthCheck check2 = injector2.getInstance(HealthCheck.class);
		assertNotSame(check1,check2);

	}

}
