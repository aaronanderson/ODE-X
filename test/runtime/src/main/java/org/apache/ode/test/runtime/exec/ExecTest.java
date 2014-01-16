package org.apache.ode.test.runtime.exec;

import static org.junit.Assert.assertNotNull;

import javax.inject.Provider;

import org.apache.ode.spi.di.DIContainer.TypeLiteral;
import org.apache.ode.spi.repo.Repository;
import org.apache.ode.spi.runtime.Node;
import org.apache.ode.spi.work.ExecutionUnit.Work;
import org.apache.ode.test.core.TestDIContainer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ExecTest {

	protected static Node node;
	protected static Provider<Work> workProvider;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		TestDIContainer container = TestDIContainer.CONTAINER.get();
		assertNotNull(container);
		node = container.getInstance(Node.class);
		assertNotNull(node);
		node.online();
		workProvider = container.getInstance(new TypeLiteral<Provider<Work>>() {
		});
		assertNotNull(workProvider);
		//operationInstance = container.getInstance(TestOperationInstanceSet.class);
		//assertNotNull(operationInstance);

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		node.offline();
	}
	
	@Test
	public void testExecSet() throws Exception {
		
	}
}
