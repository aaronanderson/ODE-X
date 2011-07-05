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
import java.io.InputStream;
import java.util.logging.Logger;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.ode.server.xml.ServerConfig;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

//import org.apache.webbeans.lifecycle.LifecycleFactory;
//import org.apache.webbeans.spi.ContainerLifecycle;

public class Server {
	public static final String OBJECTNAME = "org.apache.ode:type=ServerStop";

	private static final Logger log = Logger.getLogger(Server.class.getName());
	
	public static void main(String[] args) {
		final Server server = new Server();
		if (args.length == 0 || "start".equalsIgnoreCase(args[0])) {
			if (args.length == 2 && "daemon".equals(args[1])) {
				Thread daemonThread = new Thread(new Runnable() {

					@Override
					public void run() {
						server.start();

					}

				}, "ODE Daemon Thread");
				daemonThread.setDaemon(true);
				daemonThread.start();
				try {
					daemonThread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				server.start();
			}
		} else if ("stop".equalsIgnoreCase(args[0])) {
			server.stop();
		} else {
			log.severe("Invalid arguements. Usage: Server <start | stop>");
			System.exit(-1);
		}
	}

	public void start() {
		// ServiceLoader<ContentType> sl =
		// ServiceLoader.load(ContentType.class);
		// .getSystemResources(fullName);
		// else
		// configs =
		// Thread.currentThread().getclassloader().getResources(fullName);

		// TODO register RepoFileType mapper and RepoCommandMap with guice
		// org.jboss.weld.environment.se.StartMain.main(new
		// String[]{"JSR","299"});
		Weld weld = new Weld();
		WeldContainer container = weld.initialize();
		JMXServer server = container.instance().select(JMXServer.class).get();
		MBeanServer mserver = server.getMBeanServer();
		try {
			mserver.registerMBean(new ServerStop(weld), new ObjectName(OBJECTNAME));
		} catch (Exception e) {
			e.printStackTrace();
		}

		log.info("Server Started");
		/*
		 * lifecycle = LifecycleFactory.getInstance().getLifecycle(); try {
		 * lifecycle.startApplication(null);We } catch (Exception e1) {
		 * e1.printStackTrace(); }
		 * 
		 * //Set<Bean<?>> beans =
		 * lifecycle.getBeanManager().getBeans("webServer"); Set<Bean<?>> beans
		 * =lifecycle.getBeanManager().getBeans(Object.class,new
		 * AnnotationLiteral<Any>(){});
		 * System. out.println("All beans: "+beans.size()); Iterator<Bean<?>> i =
		 * beans.iterator(); while(i.hasNext()){ Bean<?> b = i.next();
		 * System. out.println("name: "+ b.getName() + " type: " +
		 * b.getBeanClass().getCanonicalName());
		 * 
		 * }
		 */
		/*
		 * if (beans.size()>0){ Bean<?> bean =beans.iterator().next();
		 * 
		 * WebServer webServer = (WebServer)
		 * lifecycle.getBeanManager().getReference(bean, WebServer.class,
		 * lifecycle.getBeanManager().createCreationalContext(bean));
		 * System. out.println("Retrieved the Web Server!! " +webServer); }else{
		 * System. out.println("Web Server not available!! "); }
		 */

		// endpoint.stop();

		// server.stop();
	}

	public void stop() {
		try {
			ServerConfig serverConfig = readConfig();
			JMXServiceURL url = JMXServer.buildJMXAddress(serverConfig);
			JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
			MBeanServerConnection jmxConnection = jmxc.getMBeanServerConnection();
			ServerStopMBean stopServer = JMX.newMBeanProxy(jmxConnection, new ObjectName(OBJECTNAME), ServerStopMBean.class);
			stopServer.stop();
			jmxc.close();
			for (int i=0; i< 15; i ++){
				try{
				jmxc = JMXConnectorFactory.connect(url, null);
				jmxc.close();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
					break;
				}
				}catch (IOException ie){
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		// ShutdownManager shutdownManager =
		// container.instance().select(ShutdownManager.class).get();
		// shutdownManager.shutdown();

		/*
		 * try { lifecycle.stopApplication(null); } catch (Exception e1) {
		 * e1.printStackTrace(); }
		 */

	}

	public static ServerConfig readConfig() throws IOException {

		String configName = System.getProperty("ode.config");
		if (configName == null) {
			configName = "META-INF/server.xml";
		} else {
			configName = "META-INF/" + configName + ".xml";
		}
		ClassLoader currentCL = Thread.currentThread().getContextClassLoader();
		InputStream is = currentCL.getResourceAsStream(configName);
		if (is != null) {
			try {
				JAXBContext jc = JAXBContext.newInstance("org.apache.ode.server.xml");
				Unmarshaller u = jc.createUnmarshaller();
				JAXBElement<ServerConfig> element = (JAXBElement<ServerConfig>) u.unmarshal(is);
				return element.getValue();

			} catch (JAXBException je) {
				throw new IOException(je);
			}
		} else {
			throw new IOException(String.format("Unable to locate config file %s on classpath\n", configName));
		}

	}

	public static interface ServerStopMBean {
	
		public void stop();
	}

	public class ServerStop implements ServerStopMBean {
		Weld weld;

		public ServerStop(Weld weld) {
			this.weld = weld;
		}

		public void stop() {
			//We will shutdown the server in a seperate thread so 
			//the JMX client can gracefully close it's connection before
			//The JMX server closes the listener
			Thread shutdownThread = new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					log.fine("Shutting down ODE Server");
					weld.shutdown();
					log.info("ODE Server Stopped");

				}

			}, "ODE Shutdown Thread");
			shutdownThread.setDaemon(false);
			shutdownThread.start();
		}
	}

}
