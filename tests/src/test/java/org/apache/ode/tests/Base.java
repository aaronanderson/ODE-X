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
package org.apache.ode.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Set;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.util.AnnotationLiteral;

import org.apache.ode.server.WebServer;
import org.apache.ode.server.cdi.Handler;
import org.apache.ode.server.cdi.JPAHandler;
import org.apache.ode.server.cdi.RepoHandler;
import org.apache.ode.server.cdi.StaticHandler;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class Base {
	private Weld weld;
	protected WeldContainer container;
	protected int port;
	
	@BeforeClass
	public void setUpBeforeClass() throws Exception {
		weld = new Weld();
		container = weld.initialize();
		
		Set<Bean<?>> beans = container.getBeanManager().getBeans(WebServer.class, new AnnotationLiteral<Any>() {
		});
		if (beans.size() > 0) {
			Bean<?> bean = beans.iterator().next();
			WebServer webserver = (WebServer)container.getBeanManager().getReference(bean, WebServer.class, container.getBeanManager().createCreationalContext(bean));
			port = webserver.getHttpPort();
		} else {
			System.out.println("Can't find class " + WebServer.class);
		}
	}

	@AfterClass
	public void tearDownAfterClass() throws Exception {
		try {
			weld.shutdown();
		} catch (NullPointerException e) {
		}
	}

	
	
}
