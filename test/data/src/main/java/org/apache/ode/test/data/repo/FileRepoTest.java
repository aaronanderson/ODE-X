package org.apache.ode.test.data.repo;

import static org.junit.Assert.assertNotNull;

import java.net.URI;

import org.apache.ode.spi.repo.Artifact;
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
		Artifact a = repo.read(new URI("test/file-repo/testCP.foo"), Artifact.class);
		assertNotNull(a);
		a = repo.read(new URI("test/file-repo/testFS.foo"), Artifact.class);
		assertNotNull(a);
		
		//repo.registerFileExtension("bar", "application/bar");

		
		//Artifact a = repo.read(id, Artifact.class);
		//assertNotNull(a);
		
	}
}
