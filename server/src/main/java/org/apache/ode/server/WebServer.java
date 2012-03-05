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

import java.net.ServerSocket;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.ode.runtime.ws.JAXWSServlet;
import org.apache.ode.server.xml.ServerConfig;

@Singleton
public class WebServer {

	@Inject
	ServerConfig serverConfig;

	@Inject
	JAXWSServlet handler;

	Tomcat server;

	int httpPort = -1;
	boolean sslEnabled = false;

	private static final Logger log = Logger.getLogger(WebServer.class.getName());

	@PostConstruct
	void init() {

		httpPort = serverConfig.getHttpPort().intValue();
		sslEnabled = serverConfig.isSslEnabled();

		try {
			if (httpPort == 0) {
				ServerSocket server = new ServerSocket(0);
				httpPort = server.getLocalPort();
				server.close();
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "", e);
		}

		try {
			log.fine("Starting webServer");
			server = new Tomcat();
			server.setPort(httpPort);

			// Context bootCtx = server.addContext("/boot", new
			// java.io.File(".").getAbsolutePath());
			Context baseCtx = server.addContext("", new java.io.File(".").getAbsolutePath());
			Tomcat.addServlet(baseCtx, "JAXWSServlet", handler);
			baseCtx.addServletMapping("/*", "JAXWSServlet");

			server.start();

			Thread tcThread = new Thread(new Runnable() {
				public void run() {
					server.getServer().await();
				}
			}, "ODE - Embedded Tomcat");
			tcThread.setDaemon(true);
			tcThread.start();
			log.log(Level.INFO, "Started webServer on port {0}", httpPort);
		} catch (Exception e) {
			log.log(Level.SEVERE, "", e);
		}
	}

	public int getHttpPort() {
		return httpPort;
	}

	public boolean isSSLEnabled() {
		return isSSLEnabled();
	}
	
	public Tomcat tomcat(){
		return server;
	}

	@PreDestroy
	void shutdown() {
		log.fine("Shutting down webServer");
		try {
			server.stop();
		} catch (Exception e) {
			log.log(Level.SEVERE, "", e);
		}
	}

}
