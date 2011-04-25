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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.util.AnnotationLiteral;
import javax.xml.namespace.QName;

import org.apache.ode.api.Repository;
import org.apache.ode.api.Repository.ArtifactId;
import org.apache.ode.server.cdi.JPAHandler;
import org.apache.ode.server.cdi.RepoHandler;
import org.apache.ode.server.cdi.StaticHandler;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class RepoTest {
	private static Weld weld;
	protected static WeldContainer container;
	protected static Bean<Repository> jmxRepoBean;
	protected static CreationalContext<Repository> jmxRepoCtx;
	protected static Repository jmxRepo;
	protected static Bean<org.apache.ode.spi.repo.Repository> repoBean;
	protected static CreationalContext<org.apache.ode.spi.repo.Repository> repoCtx;
	protected static org.apache.ode.spi.repo.Repository repo;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		StaticHandler.clear();
		StaticHandler.addDelegate(new JPAHandler());
		StaticHandler.addDelegate(new RepoHandler());
		weld = new Weld();
		container = weld.initialize();

		Set<Bean<?>> beans = container.getBeanManager().getBeans(org.apache.ode.spi.repo.Repository.class, new AnnotationLiteral<Any>() {
		});
		if (beans.size() > 0) {
			repoBean = (Bean<org.apache.ode.spi.repo.Repository>) beans.iterator().next();
			repoCtx = container.getBeanManager().createCreationalContext(repoBean);
			repo = (org.apache.ode.spi.repo.Repository) container.getBeanManager().getReference(repoBean, org.apache.ode.spi.repo.Repository.class, repoCtx);
		} else {
			System.out.println("Can't find class " + org.apache.ode.spi.repo.Repository.class);
		}

		beans = container.getBeanManager().getBeans(Repository.class, new AnnotationLiteral<Any>() {
		});
		if (beans.size() > 0) {
			jmxRepoBean = (Bean<Repository>) beans.iterator().next();
			jmxRepoCtx = container.getBeanManager().createCreationalContext(jmxRepoBean);
			jmxRepo = (Repository) container.getBeanManager().getReference(jmxRepoBean, Repository.class, jmxRepoCtx);
		} else {
			System.out.println("Can't find class " + Repository.class);
		}
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		if (repo != null) {
			repoBean.destroy(repo, repoCtx);
		}
		if (jmxRepo != null) {
			jmxRepoBean.destroy(jmxRepo, jmxRepoCtx);
		}
		try {
			weld.shutdown();
		} catch (NullPointerException e) {
		}
	}

	@Test
	public void testRepo() throws Exception {
		assertNotNull(repo);
		repo.registerFileExtension("bar", "application/bar");
		assertNotNull(jmxRepo);
		ArtifactId id = new ArtifactId("{http://bar.com/bar}", "application/bar", "1.0");
		id = jmxRepo.importArtifact(id, "foo.bar", false, false, "this is some bar".getBytes());
		assertNotNull(id);
		assertEquals(id.getName(), "{http://bar.com/bar}");
		assertEquals(id.getVersion(), "1.0");
		assertEquals(id.getType(), "application/bar");
		jmxRepo.refreshArtifact(id, false, "this is some foo bar".getBytes());
		byte[] contents = jmxRepo.exportArtifact(id);
		assertNotNull(contents);
		assertEquals("this is some foo bar", new String(contents));
		jmxRepo.removeArtifact(id);
		assertFalse(repo.exists(QName.valueOf(id.getName()),id.getType(), id.getVersion()));
	}

}
