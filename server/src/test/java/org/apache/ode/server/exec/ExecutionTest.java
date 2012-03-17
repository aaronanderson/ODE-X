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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import org.apache.ode.server.Server;
import org.apache.ode.server.cdi.JPAHandler;
import org.apache.ode.server.cdi.RepoHandler;
import org.apache.ode.server.cdi.RuntimeHandler;
import org.apache.ode.server.cdi.StaticHandler;
import org.apache.ode.server.exec.ClusterComponent.CancelTestAction;
import org.apache.ode.server.exec.ClusterComponent.LocalTestAction;
import org.apache.ode.server.exec.ClusterComponent.LogTestAction;
import org.apache.ode.server.exec.ClusterComponent.MasterTestAction;
import org.apache.ode.server.exec.ClusterComponent.RemoteTestAction;
import org.apache.ode.server.exec.ClusterComponent.SlaveTestAction;
import org.apache.ode.server.exec.ClusterComponent.StatusTestAction;
import org.apache.ode.spi.exec.Action;
import org.apache.ode.spi.exec.Action.TaskType;
import org.apache.ode.spi.exec.ActionTask.ActionId;
import org.apache.ode.spi.exec.ActionTask.ActionMessage;
import org.apache.ode.spi.exec.ActionTask.ActionMessage.LogType;
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

public class ExecutionTest {
	private static Server server;
	protected static ExecutionComponent execComponent;

	final static String clusterId = "test-cluster";
	final static String nodeId = "testNode1";
	final static String testNodeId = "testNode2";

	private static final Logger log = Logger.getLogger(ExecutionTest.class.getName());

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		// org.h2.tools.Server server =
		// org.h2.tools.Server.createTcpServer("-tcpPort", "9081");
		// server.start();

		StaticHandler.clear();
		StaticHandler.addDelegate(new RuntimeHandler() {

			@Override
			public void beforeBeanDiscovery(BeforeBeanDiscovery bbd, BeanManager bm) {
				bbd.addAnnotatedType(bm.createAnnotatedType(ExecutionComponent.class));
				super.beforeBeanDiscovery(bbd, bm);
			}

			@Override
			public void afterDeploymentValidation(AfterDeploymentValidation adv, BeanManager bm) {
				this.manage(ExecutionComponent.class);
				execComponent = (ExecutionComponent) getInstance(ExecutionComponent.class);
			}

			@Override
			public void beforeShutdown(BeforeShutdown adv, BeanManager bm) {
				super.stop();
			}
		});
		server = new Server();
		server.start();

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		server.stop();
	}

	@Test
	public void testInstructionInjection() throws Exception {
		assertNotNull(execComponent);

	}

}
