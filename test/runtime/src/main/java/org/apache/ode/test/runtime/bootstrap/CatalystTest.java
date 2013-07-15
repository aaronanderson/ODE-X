package org.apache.ode.test.runtime.bootstrap;

import static org.junit.Assert.assertNotNull;

import org.apache.ode.spi.exec.Node;
import org.apache.ode.spi.repo.Repository;
import org.apache.ode.test.core.TestDIContainer;
import org.junit.BeforeClass;
import org.junit.Test;

public class CatalystTest {

	protected static Repository repo;
	protected static Node node;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		TestDIContainer container = TestDIContainer.CONTAINER.get();
		assertNotNull(container);

	}

	@Test
	public void testCatalyst() throws Exception {

	}
}
