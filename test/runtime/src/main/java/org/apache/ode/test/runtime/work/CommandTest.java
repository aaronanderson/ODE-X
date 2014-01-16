package org.apache.ode.test.runtime.work;

import static org.apache.ode.test.runtime.work.CommandTest.TestCommandSet.COMMAND_NAMESPACE;
import static org.apache.ode.test.runtime.work.CommandTest.TestOperationSet.OPERATION_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.TimeUnit;

import javax.inject.Provider;
import javax.xml.namespace.QName;

import org.apache.ode.spi.di.DIContainer.TypeLiteral;
import org.apache.ode.spi.runtime.Node;
import org.apache.ode.spi.work.Command;
import org.apache.ode.spi.work.ExecutionUnit.ExecutionUnitException;
import org.apache.ode.spi.work.ExecutionUnit.ExecutionUnitState;
import org.apache.ode.spi.work.ExecutionUnit.InBuffer;
import org.apache.ode.spi.work.ExecutionUnit.InOutExecution;
import org.apache.ode.spi.work.ExecutionUnit.OutBuffer;
import org.apache.ode.spi.work.ExecutionUnit.Work;
import org.apache.ode.spi.work.Operation;
import org.apache.ode.spi.work.Operation.IO;
import org.apache.ode.test.core.TestDIContainer;
import org.junit.BeforeClass;
import org.junit.Test;

public class CommandTest {

	protected static Node node;
	protected static Provider<Work> workProvider;

	//static TestOperationInstanceSet operationInstance;

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

	@Test
	public void testInOut() throws Exception {
		Work e = workProvider.get();
		Object[] inout = new Object[1];
		inout[0] = 1;

		InOutExecution ex = e.inOutCmd(new QName(COMMAND_NAMESPACE, "InOutOperation"));
		assertNotNull(ex);
		ex.pipeIn(e.newOutput(inout));
		ex.pipeOut(e.newInput(inout));
		e.submit();
		ExecutionUnitState state = e.state(3000, TimeUnit.MILLISECONDS, ExecutionUnitState.COMPLETE);
		assertEquals(ExecutionUnitState.COMPLETE, state);
		assertEquals(2, inout[0]);

	}

	@Test
	public void testBuffer() throws Exception {
		Work e = workProvider.get();
		BufferOpOS bos = new BufferOpOS();
		bos.out = 1;
		BufferOpIS bis = new BufferOpIS();

		InOutExecution ex = e.inOutCmd(new QName(COMMAND_NAMESPACE, "BufferOperation"));
		assertNotNull(ex);
		ex.pipeIn(e.newOutput(bos));
		ex.pipeOut(e.newInput(bis));
		e.submit();
		ExecutionUnitState state = e.state(3000, TimeUnit.MILLISECONDS, ExecutionUnitState.COMPLETE);
		assertEquals(ExecutionUnitState.COMPLETE, state);
		assertEquals(1, bis.in);

	}



	public static class BufferOpIS implements InBuffer {

		public int in;

	}

	public static class BufferOpOS implements OutBuffer {

		public int out;

	}

	//TODO not sure why fully qualified name is needed here but won't compile without it. Will work if class is extracted to new file
	@org.apache.ode.spi.work.Operation.OperationSet(namespace = OPERATION_NAMESPACE, commandNamespace = COMMAND_NAMESPACE)
	@org.apache.ode.spi.work.Command.CommandSetRef(TestCommandSet.class)
	public static class TestOperationSet {
		public static final String OPERATION_NAMESPACE = "http://ode.apache.org/operations/cmdtest";

		@Operation(name = "InOutCmdOperation", command=@Command(name="InOutCommand"))
		public static void inoutOp(@IO int[] inout) throws ExecutionUnitException {
			assertNotNull(inout);
			assertEquals(1, inout.length);
			inout[0] = 2;
		}

		@Operation(name = "BufferCmdOperation")
		public static void buffOp(BufferOpIS in, BufferOpOS out) throws ExecutionUnitException {
			assertNotNull(out.out);
			out.out = in.in;
		}

	}

	@org.apache.ode.spi.work.Command.CommandSet(namespace = COMMAND_NAMESPACE)
	public static interface TestCommandSet {
		public static final String COMMAND_NAMESPACE = "http://ode.apache.org/command/test";

		@Command(name = "BufferCommand")
		public void operation(BufferOpOS out, BufferOpIS in);

		@Command(name = "InOutCommand")
		public void inoutOp(@IO int[] inout) throws ExecutionUnitException;

	}

}
