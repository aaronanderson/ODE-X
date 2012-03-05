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
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.AnnotationLiteral;
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
import org.apache.ode.spi.repo.Repository;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

//import org.apache.webbeans.lifecycle.LifecycleFactory;
//import org.apache.webbeans.spi.ContainerLifecycle;

public class Server {
	public static final String OBJECTNAME = "org.apache.ode:type=ServerStop";

	private static final Logger log = Logger.getLogger(Server.class.getName());

	private Weld weld;
	private WeldContainer container;

	public static void main(String[] args) {
		final Server server = new Server();
		if (args.length == 0 || "start".equalsIgnoreCase(args[0])) {
			server.startDeamon();
		} else if ("stop".equalsIgnoreCase(args[0])) {
			server.stopDeamon();
		} else {
			log.severe("Invalid arguements. Usage: Server <start | stop>");
			System.exit(-1);
		}
	}

	public void startDeamon() {
		Thread daemonThread = new Thread(new Runnable() {

			@Override
			public void run() {
				start();
			}

		}, "ODE Daemon Thread");
		daemonThread.setDaemon(true);
		daemonThread.start();
		try {
			daemonThread.join();
		} catch (InterruptedException e) {
			log.log(Level.SEVERE, "", e);
		}
		JMXServer server = container.instance().select(JMXServer.class).get();
		MBeanServer mserver = server.getMBeanServer();
		try {
			mserver.registerMBean(new ServerStop(), new ObjectName(OBJECTNAME));
		} catch (Exception e) {
			log.log(Level.SEVERE, "", e);
		}

	}

	public void stopDeamon() {
		try {
			ServerConfig serverConfig = readConfig();
			JMXServiceURL url = JMXServer.buildJMXAddress(serverConfig);
			JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
			MBeanServerConnection jmxConnection = jmxc.getMBeanServerConnection();
			ServerStopMBean stopServer = JMX.newMBeanProxy(jmxConnection, new ObjectName(OBJECTNAME), ServerStopMBean.class);
			stopServer.shutdown();
			jmxc.close();
			/*for (int i = 0; i < 40; i++) {
				try {
					jmxc = JMXConnectorFactory.connect(url, null);
					jmxc.close();
					try {
						Thread.sleep(250);
					} catch (InterruptedException e) {
						log.log(Level.SEVERE, "", e);
						break;
					}
				} catch (IOException ie) {
					break;
				}
			}*/
		} catch (Exception e) {
			log.log(Level.SEVERE, "", e);
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
		weld = new Weld();
		container = weld.initialize();

		log.info("Server Started");
		/*
		 * lifecycle = LifecycleFactory.getInstance().getLifecycle(); try {
		 * lifecycle.startApplication(null);We } catch (Exception e1) {
		 * e1. printStackTrace(); }
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

		// ShutdownManager shutdownManager =
		// container.instance().select(ShutdownManager.class).get();
		// shutdownManager.shutdown();
		weld.shutdown();
		log.info("ODE Server Stopped");
		/*
		 * try { lifecycle.stopApplication(null); } catch (Exception e1) {
		 * e1. printStackTrace(); }
		 */

	}

	public BeanManager getBeanManager() {
		return container.getBeanManager();
	}

	public <C> C getBeanInstance(Class<C> clazz) {
		Set<Bean<?>> beans = container.getBeanManager().getBeans(clazz, new AnnotationLiteral<Any>() {
		});
		if (beans.size() > 0) {
			Bean<C> bean = (Bean<C>) beans.iterator().next();
			CreationalContext<C> ctx = container.getBeanManager().createCreationalContext(bean);
			return (C) container.getBeanManager().getReference(bean, clazz, ctx);
		}
		return null;
	}

	public Event<Object> createEvent() {
		return container.event();
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

		public void shutdown();
	}

	public class ServerStop implements ServerStopMBean {

		public void shutdown() {
			/* We will run the shutdown in a separate thread so that the remote
			 * client can successfully disconnect. The JMX clients should gracefully 
			 * close their connections within 10 seconds of a shutdown request 
			 * before The JMX server closes the server listener
			
			 */
			Thread shutdownThread = new Thread(new Runnable() {

				@Override
				public void run() {
					log.info("Remote shutdown of ODE Server");
					stop();
				}

			}, "ODE Shutdown Thread");
			shutdownThread.setDaemon(false);
			shutdownThread.start();

		}
	}

}
