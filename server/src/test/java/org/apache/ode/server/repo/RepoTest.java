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
package org.apache.ode.server.repo;

import static org.junit.Assert.*;

import java.util.Set;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;

import org.apache.ode.api.Repository;
import org.apache.ode.api.Repository.ArtifactId;
import org.apache.ode.runtime.jmx.RepositoryImpl;
import org.apache.ode.server.WebServer;
import org.apache.ode.server.cdi.Handler;
import org.apache.ode.server.cdi.JPAHandler;
import org.apache.ode.server.cdi.RepoHandler;
import org.apache.ode.server.cdi.StaticHandler;
import org.apache.ode.server.plugin.BarPlugin;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class RepoTest {
	private static Weld weld;
	protected static WeldContainer container;
	protected static Repository repo = null;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		StaticHandler.clear();
		StaticHandler.addDelegate(new JPAHandler());
		StaticHandler.addDelegate(new RepoHandler());
		StaticHandler.addDelegate(new Handler() {

			public void beforeBeanDiscovery(BeforeBeanDiscovery bbd, BeanManager bm) {
				bbd.addAnnotatedType(bm.createAnnotatedType(RepositoryImpl.class));
				bbd.addAnnotatedType(bm.createAnnotatedType(BarPlugin.class));

			}
		});
		weld = new Weld();
		container = weld.initialize();
		
		Set<Bean<?>> beans = container.getBeanManager().getBeans(Repository.class, new AnnotationLiteral<Any>() {
		});
		if (beans.size() > 0) {
			Bean<?> bean = beans.iterator().next();
			repo = (Repository)container.getBeanManager().getReference(bean, WebServer.class, container.getBeanManager().createCreationalContext(bean));
		} else {
			System.out.println("Can't find class " + Repository.class);
		}
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		try {
			weld.shutdown();
		} catch (NullPointerException e) {
		}
	}

	@Test
	public void testImportExport() throws Exception{
		
		assertNotNull(repo);
		ArtifactId id = repo.importFile("{http://foo.com/foo}", "1.0", "foo.bar", "this is some foo".getBytes());
		//assertNotNull(id);
		//assertEquals(id.getName(), "{http://foo.com/foo}");
		//assertEquals(id.getVersion(), "1.0");
		//assertEquals(id.getType(), "bar");
		
	}

}
