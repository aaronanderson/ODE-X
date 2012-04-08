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
package org.apache.ode.server.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.CommandInfo;
import javax.activation.DataHandler;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.util.AnnotationLiteral;
import javax.xml.namespace.QName;

import org.apache.ode.server.Server;
import org.apache.ode.server.cdi.JPAHandler;
import org.apache.ode.server.cdi.RepoHandler;
import org.apache.ode.server.cdi.StaticHandler;
import org.apache.ode.server.plugin.BarPlugin.Bar;
import org.apache.ode.spi.cdi.Handler;
import org.apache.ode.spi.repo.Repository;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class PluginTest {
	private static Server server;
	protected static BarPlugin barPlugin;

	private static final Logger log = Logger.getLogger(PluginTest.class.getName());

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		StaticHandler.clear();
		StaticHandler.addDelegate(new JPAHandler());
		StaticHandler.addDelegate(new RepoHandler());
		StaticHandler.addDelegate(new Handler() {
			@Override
			public void beforeBeanDiscovery(BeforeBeanDiscovery bbd, BeanManager bm) {
				bbd.addAnnotatedType(bm.createAnnotatedType(BarPlugin.class));
				bbd.addAnnotatedType(bm.createAnnotatedType(BarPlugin.BarValidation.class));

			}

			@Override
			public void afterDeploymentValidation(AfterDeploymentValidation adv, BeanManager bm) {
				manage(BarPlugin.class);
				start(bm);
			}

			@Override
			public void beforeShutdown(BeforeShutdown adv, BeanManager bm) {
				stop();
			}

		});
		server = new Server();
		server.start();

		Set<Bean<?>> beans = server.getBeanManager().getBeans(BarPlugin.class, new AnnotationLiteral<Any>() {
		});
		if (beans.size() > 0) {
			Bean<?> bean = beans.iterator().next();
			barPlugin = (BarPlugin) server.getBeanManager().getReference(bean, BarPlugin.class, server.getBeanManager().createCreationalContext(bean));
		} else {
			log.log(Level.SEVERE, "Can't find class {0}", BarPlugin.class);
		}
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		server.stop();
	}

	@Test
	public void testPlugin() throws Exception {
		assertNotNull(barPlugin);
	}

	@Test
	public void testMimeType() throws Exception {
		assertEquals("application/bar", barPlugin.getArtifactDataSource("test.bar").getContentType());
		assertEquals("application/bar", barPlugin.getArtifactDataSource("test.bar2").getContentType());
	}

	@Test
	public void testMimeMatching() throws Exception {
		File f = new File("target/test-classes/plugin/test.bar");
		DataHandler dh = barPlugin.getDataHandlerByFilename(f);
		assertNotNull(dh);
		assertEquals(BarPlugin.BAR_MIMETYPE, dh.getContentType());
		f = new File("target/test-classes/plugin/test.foo");
		dh = barPlugin.getDataHandlerByContent(f);
		assertNotNull(dh);
		assertEquals(BarPlugin.FOO_MIMETYPE, dh.getContentType());
	}

	@Test
	public void testCommand() throws Exception {
		File f = new File("target/test-classes/plugin/test.bar");
		DataHandler dh = barPlugin.getDataHandlerByFilename(f);
		assertNotNull(dh);
		CommandInfo ci = dh.getCommand("validate");
		assertNotNull(ci);
		Object o = dh.getBean(ci);
		assertTrue(o instanceof BarPlugin.BarValidation);
		BarPlugin.BarValidation val = (BarPlugin.BarValidation) o;
		assertTrue(val.validate(null));
	}

	@Test
	public void testCreate() throws Exception {
		Repository repo = barPlugin.getRepository();
		assertNotNull(repo);
		BarPlugin.Bar bar = new BarPlugin.Bar("Baz");
		repo.create(QName.valueOf("{http://bar/store}"), BarPlugin.BAR_MIMETYPE, "1.0", bar);
	}

	@Test
	public void testRead() throws Exception {
		Repository repo = barPlugin.getRepository();
		assertNotNull(repo);
		BarPlugin.Bar bar = new BarPlugin.Bar();
		bar.setPayload("Foo");
		repo.create(QName.valueOf("{http://bar/load}"), BarPlugin.BAR_MIMETYPE, "1.0", bar);
		bar = repo.read(QName.valueOf("{http://bar/load}"), BarPlugin.BAR_MIMETYPE, "1.0", Bar.class);
		assertNotNull(bar);
		assertEquals("Foo", bar.getPayload());
	}

}
