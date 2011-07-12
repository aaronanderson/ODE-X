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

import static org.apache.ode.spi.repo.DataContentHandler.readStream;

import org.apache.ode.api.BuildSystem;
import org.apache.ode.api.Repository;
import org.apache.ode.api.Repository.ArtifactId;
import org.apache.ode.bpel.plugin.cdi.BPELHandler;
import org.apache.ode.server.Server;
import org.apache.ode.server.cdi.JPAHandler;
import org.apache.ode.server.cdi.RepoHandler;
import org.apache.ode.server.cdi.RuntimeHandler;
import org.apache.ode.server.cdi.StaticHandler;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class CompilerTest {
	
	private static Server server;
	private static BuildSystem buildSystem;
	private static Repository repo;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		StaticHandler.clear();
		StaticHandler.addDelegate(new JPAHandler());
		StaticHandler.addDelegate(new RepoHandler());
		StaticHandler.addDelegate(new RuntimeHandler());
		StaticHandler.addDelegate(new BPELHandler());
		
		server = new Server();
		server.start();
		buildSystem = server.getBeanInstance(BuildSystem.class);
		repo = server.getBeanInstance(Repository.class);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		server.stop();
	}

	@Test
	public void testCompileHelloWorld() throws Exception {
		repo.importArtifact(new ArtifactId("{http://ode/bpel/unit-test.wsdl}HelloService", null, null), "HelloWorld.wsdl", true, true, readStream(Thread.currentThread().getContextClassLoader().getResourceAsStream("HelloWorld/HelloWorld.wsdl")));
		repo.importArtifact(new ArtifactId("{http://ode/bpel/unit-test}HelloWorld", null, null), "HelloWorld.bpel", true, true, readStream(Thread.currentThread().getContextClassLoader().getResourceAsStream("HelloWorld/HelloWorld.bpel")));
		buildSystem.build(readStream(Thread.currentThread().getContextClassLoader().getResourceAsStream("HelloWorld/HelloWorld.build")));
	}

}
