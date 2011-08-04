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
package org.apache.ode.bpmn.tests;

import org.apache.ode.api.BuildSystem;
import org.apache.ode.api.Repository;
import org.apache.ode.bpmn.plugin.cdi.BPMNHandler;
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
		StaticHandler.addDelegate(new BPMNHandler());

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
	/*	repo.importArtifact(new ArtifactId("{http://ode/bpel/unit-test.wsdl}HelloService", null, null), "HelloWorld.wsdl", true, true, readStream(Thread
				.currentThread().getContextClassLoader().getResourceAsStream("HelloWorld/HelloWorld.wsdl")));
		repo.importArtifact(new ArtifactId("{http://ode/bpel/unit-test}HelloWorld", null, null), "HelloWorld.bpel", true, true, readStream(Thread
				.currentThread().getContextClassLoader().getResourceAsStream("HelloWorld/HelloWorld.bpel")));
		buildSystem.build(readStream(Thread.currentThread().getContextClassLoader().getResourceAsStream("HelloWorld/HelloWorld.build")));
		byte[] content = repo.exportArtifact(new ArtifactId("{http://ode/bpel/unit-test}HelloWorld", "application/ode-executable", "1.0"));
		assertNotNull(content);
		System.out.println(new String(content));
		Document doc = ParserUtils.contentToDom(content);
		XPathFactory xpFactory = XPathFactory.newInstance();
		XPath xPath = xpFactory.newXPath();
		Map<String,String> ns = new HashMap<String,String>();
		ns.put("e", Platform.EXEC_NAMESPACE);
		ns.put("b", BPELComponent.BPEL_INSTRUCTION_SET_NS);
		NamespaceContext ctx = ParserUtils.buildNSContext(ns);
		xPath.setNamespaceContext(ctx);
		assertNotNull(xPath.evaluate("/e:executable/e:block[1]/b:process[1]", doc,XPathConstants.NODE));
		assertNotNull(xPath.evaluate("/e:executable/e:block[1]/b:process[2]", doc,XPathConstants.NODE));
	*/
	}

	@Test
	public void testLocatorPreProcessor() throws Exception {
		/*
		byte[] bpel = readStream(Thread
				.currentThread().getContextClassLoader().getResourceAsStream("HelloWorld/HelloWorld.bpel"));
		Document doc = ParserUtils.inlineLocation(bpel, Collections.EMPTY_SET);
		assertNotNull(doc);
		Element p = (Element)doc.getDocumentElement();
		assertNotNull(p);
		Attr a = p.getAttributeNodeNS(ParserUtils.LOCATION_NS, ParserUtils.LOCATION_START_LINE_ATTR);
		assertNotNull(a);
		assertEquals("13",a.getTextContent());
		a = p.getAttributeNodeNS(ParserUtils.LOCATION_NS, ParserUtils.LOCATION_END_LINE_ATTR);
		assertNotNull(a);
		assertEquals("64",a.getTextContent());
		//System.out.println(ParserUtils.domToString(doc));
		 */
	}
	
}
