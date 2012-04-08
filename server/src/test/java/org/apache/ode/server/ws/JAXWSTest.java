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
package org.apache.ode.server.ws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.util.AnnotationLiteral;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Service;

import org.apache.ode.runtime.ws.JAXWSHandler;
import org.apache.ode.runtime.ws.JAXWSHttpContext;
import org.apache.ode.server.WebServer;
import org.apache.ode.server.cdi.StaticHandler;
import org.apache.ode.server.cdi.WebServerHandler;
import org.apache.ode.server.xml.ServerConfig;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class JAXWSTest {
	private static Weld weld;
	protected static WeldContainer container;
	protected static Bean<WebServer> wsBean;
	protected static CreationalContext<WebServer> wsCtx;
	protected static WebServer ws;
	protected static Bean<JAXWSHandler> jwsBean;
	protected static CreationalContext<JAXWSHandler> jwsCtx;
	protected static JAXWSHandler jws;
	protected static Bean<ServerConfig> cfgBean;
	protected static CreationalContext<ServerConfig> cfgCtx;
	protected static ServerConfig cfg;

	private static final Logger log = Logger.getLogger(JAXWSTest.class.getName());

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		StaticHandler.clear();
		StaticHandler.addDelegate(new WebServerHandler());
		weld = new Weld();
		container = weld.initialize();

		Set<Bean<?>> beans = container.getBeanManager().getBeans(WebServer.class, new AnnotationLiteral<Any>() {
		});
		if (beans.size() > 0) {
			wsBean = (Bean<WebServer>) beans.iterator().next();
			wsCtx = container.getBeanManager().createCreationalContext(wsBean);
			ws = (WebServer) container.getBeanManager().getReference(wsBean, WebServer.class, wsCtx);
		} else {
			log.log(Level.SEVERE, "Can't find class {0}", WebServer.class);
		}
		beans = container.getBeanManager().getBeans(JAXWSHandler.class, new AnnotationLiteral<Any>() {
		});
		if (beans.size() > 0) {
			jwsBean = (Bean<JAXWSHandler>) beans.iterator().next();
			jwsCtx = container.getBeanManager().createCreationalContext(jwsBean);
			jws = (JAXWSHandler) container.getBeanManager().getReference(jwsBean, JAXWSHandler.class, jwsCtx);
		} else {
			log.log(Level.SEVERE, "Can't find class {0}", JAXWSHandler.class);
		}
		beans = container.getBeanManager().getBeans(ServerConfig.class, new AnnotationLiteral<Any>() {
		});
		if (beans.size() > 0) {
			cfgBean = (Bean<ServerConfig>) beans.iterator().next();
			cfgCtx = container.getBeanManager().createCreationalContext(cfgBean);
			cfg = (ServerConfig) container.getBeanManager().getReference(cfgBean, ServerConfig.class, cfgCtx);
		} else {
			log.log(Level.SEVERE, "Can't find class {0}" + ServerConfig.class);
		}

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		if (ws != null) {
			wsBean.destroy(ws, wsCtx);
		}
		if (jws != null) {
			jwsBean.destroy(jws, jwsCtx);
		}
		if (cfg != null) {
			cfgBean.destroy(cfg, cfgCtx);
		}
		try {
			weld.shutdown();
		} catch (NullPointerException e) {
		}
	}

	@Test
	public void invalid() throws Exception {
		assertNotNull(ws);
		String ctxPath = "/echo";
		String epPath = "/service";
		String endpointAddr = "http://" + cfg.getHost() + ":" + cfg.getHttpPort() + ctxPath + epPath;
		URL wsdlURL = new URL(endpointAddr + "?wsdl");
		HttpURLConnection wsdlConnection = (HttpURLConnection) wsdlURL.openConnection();
		wsdlConnection.connect();
		assertEquals(HttpServletResponse.SC_NOT_FOUND, wsdlConnection.getResponseCode());
	}

	@Test
	public void duplicate() throws Exception {
		String ctxPath = "/echo";
		String epPath = "/service";
		JAXWSHttpContext ctx = jws.register(ctxPath, epPath);
		try {
			jws.register(ctxPath, epPath);
			fail();
		} catch (Exception e) {
		} finally {
			jws.unregister(ctx);
		}
	}

	@Test
	public void testEcho() throws Exception {
		ServerSocket portNum = new ServerSocket(0);
		int port = portNum.getLocalPort();
		portNum.close();
		String ctxPath = "/echo";
		String epPath = "/service";
		String endpointAddr = "http://" + cfg.getHost() + ":" + cfg.getHttpPort() + ctxPath + epPath;

		JAXWSHttpContext context = jws.register(ctxPath, epPath);
		Endpoint endpoint = Endpoint.create(new EchoServiceImpl());
		endpoint.publish(context);

		URL wsdlURL = new URL(endpointAddr + "?wsdl");
		HttpURLConnection wsdlConnection = (HttpURLConnection) wsdlURL.openConnection();
		wsdlConnection.connect();
		assertEquals(HttpServletResponse.SC_OK, wsdlConnection.getResponseCode());
		StringBuilder wsdl = new StringBuilder();
		BufferedReader reader = new BufferedReader(new InputStreamReader(wsdlConnection.getInputStream()));
		String line = null;
		while ((line = reader.readLine()) != null) {
			wsdl.append(line);
			wsdl.append("\n");
		}
		// System.out.println(wsdl);
		assertTrue(wsdl.toString().contains("name=\"EchoService\""));

		QName serviceName = new QName(EchoService.TNS, EchoServiceImpl.SERVICE_NAME);
		QName portName = new QName(EchoService.TNS, EchoServiceImpl.PORT_NAME);
		// Service service = Service.create(serviceName);
		// service.addPort(portName, SOAPBinding.SOAP11HTTP_BINDING,
		// endpointAddr);
		Service service = Service.create(wsdlURL, serviceName);
		EchoService echoService = (EchoService) service.getPort(portName, EchoService.class);
		assertEquals("echo: test message", echoService.echo("test message"));

		endpoint.stop();
		jws.unregister(context);
	}

}
