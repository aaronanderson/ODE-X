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
package org.apache.ode.cli;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.rmi.registry.LocateRegistry;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.ode.api.Repository;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.beust.jcommander.JCommander;

public class CLITest {
	static JMXConnectorServer cntorServer;
	static int port;

	private static final Logger log = Logger.getLogger(CLITest.class.getName());

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		ServerSocket server = new ServerSocket(0);
		port = server.getLocalPort();
		server.close();
		LocateRegistry.createRegistry(port); 
		log.info("Registry created");
		JMXServiceURL address = new JMXServiceURL("service:jmx:rmi://localhost:"+port+"/jndi/rmi://localhost:"+port+"/jmxrmi"); 
		log.log(Level.INFO,"JMXServer address:{0} ",  address);
		Map environment = null;
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		cntorServer = JMXConnectorServerFactory.newJMXConnectorServer(address, environment, mbs);
		
		//mbs.registerMBean(new MachineImpl(), ObjectName.getInstance(Machine.OBJECTNAME));
		mbs.registerMBean(new RepositoryImpl(), ObjectName.getInstance(Repository.OBJECTNAME));
		cntorServer.start();
		log.info("JMXServer started");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		mbs.unregisterMBean(ObjectName.getInstance(Repository.OBJECTNAME));
		// mbs.unregisterMBean(ObjectName.getInstance(Machine.OBJECTNAME));
		cntorServer.stop();
		log.info("JMXServer stopped");
	}

	@Test
	public void testConnection() throws Exception {
		Connection con = new Connection();
		JCommander cmd = new JCommander(con);
		cmd.parse("--port", String.valueOf(port));
		assertNotNull(con.getConnection());

	}

	@Test
	public void testImport() {
		StringBuilder out = new StringBuilder();
		CLI.execute(out, "--port", String.valueOf(port), "import", "--file", "target/test-classes/import.txt");
		assertTrue(out.toString().contains("Sucessfull"));
		out = new StringBuilder();

	}

}