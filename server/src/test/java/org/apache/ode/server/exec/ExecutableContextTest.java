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
package org.apache.ode.server.exec;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.ode.server.Server;
import org.apache.ode.server.cdi.ExecutableScopeContextImpl;
import org.apache.ode.server.cdi.JPAHandler;
import org.apache.ode.server.cdi.RepoHandler;
import org.apache.ode.server.cdi.RuntimeHandler;
import org.apache.ode.server.cdi.StaticHandler;
import org.apache.ode.server.exec.instruction.AttributeInstruction;
import org.apache.ode.server.exec.instruction.ExecutableShared;
import org.apache.ode.server.exec.instruction.ScopeInstruction;
import org.apache.ode.server.exec.instruction.TestObjectFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ExecutableContextTest {
	private static Server server;
	protected static ExecutionComponent execComponent;
	protected static ScopeProvider provider;

	private static final Logger log = Logger.getLogger(ExecutableContextTest.class.getName());

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		// org.h2.tools.Server server =
		// org.h2.tools.Server.createTcpServer("-tcpPort", "9081");
		// server.start();

		StaticHandler.clear();
		StaticHandler.addDelegate(new JPAHandler());
		StaticHandler.addDelegate(new RepoHandler());
		StaticHandler.addDelegate(new RuntimeHandler() {

			@Override
			public void beforeBeanDiscovery(BeforeBeanDiscovery bbd, BeanManager bm) {
				bbd.addAnnotatedType(bm.createAnnotatedType(ExecutionComponent.class));
				bbd.addAnnotatedType(bm.createAnnotatedType(ScopeProvider.class));
				bbd.addAnnotatedType(bm.createAnnotatedType(TestObjectFactory.class));
				bbd.addAnnotatedType(bm.createAnnotatedType(ScopeInstruction.class));
				bbd.addAnnotatedType(bm.createAnnotatedType(AttributeInstruction.class));
				bbd.addAnnotatedType(bm.createAnnotatedType(ExecutableShared.class));
				super.beforeBeanDiscovery(bbd, bm);
			}

			@Override
			public void afterDeploymentValidation(AfterDeploymentValidation adv, BeanManager bm) {
				this.manage(ExecutionComponent.class);
				this.manage(ScopeProvider.class);
				super.afterDeploymentValidation(adv, bm);
				execComponent = (ExecutionComponent) getInstance(ExecutionComponent.class);
				provider = (ScopeProvider) getInstance(ScopeProvider.class);
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

	@Singleton
	public static class ScopeProvider {

		@Inject
		Provider<ExecutableScopeContextImpl> ctx;

	}

	@Test
	public void testExecutableScope() throws Exception {
		assertNotNull(execComponent);
		assertNotNull(provider);
		ExecutableScopeContextImpl esc1 = provider.ctx.get();
		assertNotNull(esc1);
		ExecutableScopeContextImpl esc2 = provider.ctx.get();
		assertNotNull(esc2);
		assertFalse(esc1.equals(esc2));
		esc1.create();
		ScopeInstruction st1_of1_esc1;
		try {
			esc1.begin();
			TestObjectFactory of1_esc1 = esc1.newInstance(TestObjectFactory.class);
			assertNotNull(of1_esc1);
			st1_of1_esc1 = (ScopeInstruction) of1_esc1.createScopeTest();
			assertNotNull(st1_of1_esc1);
			assertTrue(st1_of1_esc1.isStarted());
			assertNotNull(st1_of1_esc1.getShared());
			ScopeInstruction st2_of1_esc1 = (ScopeInstruction) of1_esc1.createScopeTest();
			assertNotNull(st2_of1_esc1);
			assertFalse(st1_of1_esc1.equals(st2_of1_esc1));
			assertTrue(st1_of1_esc1.getShared().equals(st2_of1_esc1.getShared()));
			TestObjectFactory of2_esc1 = esc1.newInstance(TestObjectFactory.class);
			assertNotNull(of2_esc1);
			assertFalse(of1_esc1.equals(of2_esc1));
			ScopeInstruction st1_of2_esc1 = (ScopeInstruction) of2_esc1.createScopeTest();
			assertNotNull(st1_of2_esc1);
			assertFalse(st1_of1_esc1.equals(st1_of2_esc1));
			assertTrue(st1_of1_esc1.getShared().equals(st1_of2_esc1.getShared()));
		} finally {
			esc1.end();
		}
		esc2.create();
		try {
			esc2.begin();
			TestObjectFactory of1_esc2 = esc2.newInstance(TestObjectFactory.class);
			assertNotNull(of1_esc2);
			ScopeInstruction st1_of1_esc2 = (ScopeInstruction) of1_esc2.createScopeTest();
			assertNotNull(st1_of1_esc2);
			assertFalse(st1_of1_esc1.getShared().equals(st1_of1_esc2.getShared()));
		} finally {
			esc2.end();
		}
		esc2.destroy();
		esc1.destroy();
		assertTrue(st1_of1_esc1.isStopped());
	}

}
