package org.apache.ode.test.data.repo;

import static org.junit.Assert.assertNotNull;

import org.apache.ode.spi.repo.Repository;
import org.apache.ode.test.core.TestDIContainer;
import org.junit.BeforeClass;
import org.junit.Test;

public class FileRepoTest {

	protected static Repository repo;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		TestDIContainer container = TestDIContainer.CONTAINER.get();
		assertNotNull(container);
		repo = container.getInstance(org.apache.ode.spi.repo.Repository.class);

	}

	@Test
	public void testFileRepo() throws Exception {
		assertNotNull(repo);
		//repo.registerFileExtension("bar", "application/bar");

		
		//Artifact a = repo.read(id, Artifact.class);
		//assertNotNull(a);
		
	}
}
