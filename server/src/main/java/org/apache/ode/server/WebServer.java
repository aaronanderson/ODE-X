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
package org.apache.ode.server;

import java.io.IOException;
import java.net.ServerSocket;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.ode.server.xml.ServerConfig;

import com.sun.grizzly.http.embed.GrizzlyWebServer;

@Singleton
public class WebServer {
	@Inject
	ServerConfig serverConfig;
	GrizzlyWebServer server;
	int httpPort = -1;
	boolean sslEnabled = false;

	@PostConstruct
	void init() {
		String contextPath = "/ctxt";
		String path = "/echo";

		httpPort = serverConfig.getHttpPort().intValue();
		sslEnabled = serverConfig.isSslEnabled();

		try {
			if (httpPort == 0) {
				ServerSocket server = new ServerSocket(0);
				httpPort = server.getLocalPort();
				server.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		String address = "http://localhost:" + httpPort + contextPath + path;

		server = new GrizzlyWebServer(httpPort);
		// SSLConfig cfg = new SSLConfig();
		// server.setSSLConfiguration(sslConfiguration)
		// HttpContext context =
		// GrizzlyHttpContextFactory.createHttpContext(server, contextPath,
		// path)t(server, contextPath, path)pContext(server, contextPath,
		// path)ontext(server, contextPath, path);
		// context.setHandler(new JAXWSHandler());
		// Endpoint endpoint = Endpoint.create(new Object());
		// endpoint.publish(context); // Use grizzly HTTP context for publishing

		try {
			System.out.println("Starting webServer");
			server.start();
			System.out.println("Started webServer");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int getHttpPort() {
		return httpPort;
	}

	public boolean isSSLEnabled() {
		return isSSLEnabled();
	}

	@PreDestroy
	void shutdown() {
		System.out.println("Shutting down webServer");
		server.stop();
	}

}
