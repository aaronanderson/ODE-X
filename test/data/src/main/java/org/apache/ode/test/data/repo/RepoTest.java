package org.apache.ode.test.data.repo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URI;

import org.apache.ode.spi.repo.Artifact;
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
		Artifact a = new Artifact().withURI(new URI("{http://bar.com/bar}")).withContentType("application/bar").withVersion("1.0")
				.withContent(new String("Original Contents").getBytes());

		repo.create(a.getURI(), a.getVersion(), a.getContentType(), a);
		a = repo.read(a.getURI(), a.getVersion(), a.getContentType(), Artifact.class);
		assertNotNull(a);
		assertEquals(a.getURI(), "{http://bar.com/bar}");
		assertEquals(a.getVersion(), "1.0");
		assertEquals(a.getContentType(), "application/bar");
		assertEquals(new String(a.getContent()), "Original Contents");
		a.setContent(new String("New Contents").getBytes());
		repo.update(a.getURI(), a.getVersion(), a.getContentType(), a);
		assertNotNull(a);
		a = repo.read(a.getURI(), a.getVersion(), a.getContentType(), Artifact.class);
		assertNotNull(a);
		assertEquals(new String(a.getContent()), "New Contents");
		repo.delete(a.getURI(), a.getVersion(), a.getContentType());
	}
}
