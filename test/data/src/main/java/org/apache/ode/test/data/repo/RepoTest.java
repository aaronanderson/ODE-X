package org.apache.ode.test.data.repo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;

import java.net.URI;
import java.util.UUID;

import org.apache.ode.spi.repo.Artifact;
import org.apache.ode.spi.repo.Criteria;
import org.apache.ode.spi.repo.Repository;
import org.apache.ode.test.core.DIContainer;
import org.junit.BeforeClass;
import org.junit.Test;

public class RepoTest {

	protected static Repository repo;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		System.out.println("Get thread" + Thread.currentThread());
		DIContainer container = DIContainer.CONTAINER.get();
		assertNotNull(container);
		//setupDIContainer(RepoTest.class);
		repo = container.getInstance(org.apache.ode.spi.repo.Repository.class);

	}

	//@AfterClass
	//public static void tearDownAfterClass() throws Exception {

	//}

	@Test
	public void testRepo() throws Exception {
		assertNotNull(repo);
		repo.registerFileExtension("bar", "application/bar");

		UUID id = repo.create(new URI("http://bar.com/bar#bar"), "1.0", "application/bar", new String("Original Contents").getBytes());
		assertNotNull(id);
		Artifact a = repo.read(id, Artifact.class);
		assertNotNull(a);
		assertEquals(new URI("http://bar.com/bar#bar"), a.getURI());
		assertEquals("1.0", a.getVersion());
		assertEquals("application/bar", a.getContentType());
		assertEquals(new String("Original Contents"),  new String(a.getContent()));
		a.setContent(new String("New Contents").getBytes());
		repo.update(a.getURI(), a.getContent());
		a = repo.read(id, Artifact.class);
		assertNotNull(a);
		assertEquals(new String("New Contents"), new String(a.getContent()));
		repo.delete(new Criteria(a.getURI(), a.getVersion(), a.getContentType()));
		assertFalse(repo.exists(id));
	}
}
