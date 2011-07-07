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
package org.apache.ode.bpel.tests;

import org.apache.ode.bpel.plugin.cdi.BPELHandler;
import org.apache.ode.server.Server;
import org.apache.ode.server.cdi.JPAHandler;
import org.apache.ode.server.cdi.RepoHandler;
import org.apache.ode.server.cdi.RuntimeHandler;
import org.apache.ode.server.cdi.StaticHandler;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


public class CompilerTest {
	
	private static Server server;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		StaticHandler.clear();
		StaticHandler.addDelegate(new JPAHandler());
		StaticHandler.addDelegate(new RepoHandler());
		StaticHandler.addDelegate(new RuntimeHandler());
		StaticHandler.addDelegate(new BPELHandler());
		
		server = new Server();
		server.start();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		server.stop();
	}

	@Test
	public void testCompileHelloWorld() throws Exception {
		
	}

}
