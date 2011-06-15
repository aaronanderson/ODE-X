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
import java.net.MalformedURLException;
import java.rmi.registry.LocateRegistry;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.ode.server.xml.ServerConfig;

@Singleton
public class JMXServer {
	@Inject
	ServerConfig serverConfig;
	JMXConnectorServer cntorServer;
	MBeanServer mbeanServer;
	int port = -1;

	@PostConstruct
	void init() {
		try {
			System.out.println("Starting jmxServer");
			Map environment = null;
			mbeanServer = MBeanServerFactory.createMBeanServer();
			port = serverConfig.getJmxPort().intValue();
			JMXServiceURL address = buildJMXAddress(serverConfig);
			LocateRegistry.createRegistry(port); 
			System.out.println("Registry created");
			// JMXServiceURL address = new
			// JMXServiceURL("service:jmx:rmi://localhost:"+port+"/jndi/rmi://localhost:"+port+"/jmxrmi");
			cntorServer = JMXConnectorServerFactory.newJMXConnectorServer(address, environment, mbeanServer);
			cntorServer.start();
			System.out.println("Started jmxServer");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static JMXServiceURL buildJMXAddress(ServerConfig serverConfig) throws MalformedURLException {
		if (serverConfig.getJmxURL() != null) {
			return new JMXServiceURL(serverConfig.getJmxURL());
		} else {
			int port = serverConfig.getJmxPort().intValue();
			String host = serverConfig.getHost();
			return new JMXServiceURL("service:jmx:rmi://" + host + ":" + port + "/jndi/rmi://" + host + ":" + port + "/jmxrmi");
		}
	}

	public int getPort() {
		return port;
	}

	public MBeanServer getMBeanServer() {
		return mbeanServer;
	}

	@PreDestroy
	void shutdown() {
		try {
			System.out.println("Stoping jmxServer");
			cntorServer.stop();
			MBeanServerFactory.releaseMBeanServer(mbeanServer);
			System.out.println("Stopped jmxServer");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
