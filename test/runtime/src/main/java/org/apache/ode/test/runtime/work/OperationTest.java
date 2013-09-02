package org.apache.ode.test.runtime.work;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.apache.ode.test.runtime.work.OperationTest.TestCommandSet.COMMAND_NAMESPACE;
import static org.apache.ode.test.runtime.work.OperationTest.TestOperationSet.OPERATION_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

import javax.inject.Provider;
import javax.inject.Qualifier;
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
import org.apache.ode.spi.work.Operation.I;
import org.apache.ode.spi.work.Operation.IO;
import org.apache.ode.spi.work.Operation.IP;
import org.apache.ode.spi.work.Operation.O;
import org.apache.ode.test.core.TestDIContainer;
import org.junit.BeforeClass;
import org.junit.Test;

public class OperationTest {

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
	public void testHolderParameters() throws Exception {
		Work e = workProvider.get();
		int[] in = new int[1];
		int[] out = new int[1];
		out[0] = 1;

		InOutExecution ex = e.inOutOp(new QName(OPERATION_NAMESPACE, "ParameterOperation"));
		assertNotNull(ex);
		ex.pipeIn(e.newOutput(out));
		ex.pipeOut(e.newInput(in));
		e.submit();
		ExecutionUnitState state = e.state(3000, TimeUnit.MILLISECONDS, ExecutionUnitState.COMPLETE);
		assertEquals(ExecutionUnitState.COMPLETE, state);
		assertEquals(1, in[0]);

	}

	@Test
	public void testArrayCoerceParameters() throws Exception {
		Work e = workProvider.get();
		Object[] in = new Object[1];
		Object[] out = new Object[1];
		out[0] = 1;

		InOutExecution ex = e.inOutOp(new QName(OPERATION_NAMESPACE, "ParameterOperation"));
		assertNotNull(ex);
		ex.pipeIn(e.newOutput(out));
		ex.pipeOut(e.newInput(in));
		e.submit();
		ExecutionUnitState state = e.state(3000, TimeUnit.MILLISECONDS, ExecutionUnitState.COMPLETE);
		assertEquals(ExecutionUnitState.COMPLETE, state);
		assertEquals(1, in[0]);

	}

	@Test
	public void testInOut() throws Exception {
		Work e = workProvider.get();
		Object[] inout = new Object[1];
		inout[0] = 1;

		InOutExecution ex = e.inOutOp(new QName(OPERATION_NAMESPACE, "InOutOperation"));
		assertNotNull(ex);
		ex.pipeIn(e.newOutput(inout));
		ex.pipeOut(e.newInput(inout));
		e.submit();
		ExecutionUnitState state = e.state(3000, TimeUnit.MILLISECONDS, ExecutionUnitState.COMPLETE);
		assertEquals(ExecutionUnitState.COMPLETE, state);
		assertEquals(2, inout[0]);

	}

	@Test
	public void testReturn() throws Exception {
		Work e = workProvider.get();
		int[] in = new int[1];

		InOutExecution ex = e.inOutOp(new QName(OPERATION_NAMESPACE, "ReturnOperation"));
		assertNotNull(ex);
		ex.pipeIn(e.newOutput(1));
		ex.pipeOut(e.newInput(in));
		e.submit();
		ExecutionUnitState state = e.state(3000, TimeUnit.MILLISECONDS, ExecutionUnitState.COMPLETE);
		assertEquals(ExecutionUnitState.COMPLETE, state);
		assertEquals(2, in[0]);

	}

	@Test
	public void testBuffer() throws Exception {
		Work e = workProvider.get();
		BufferOpOS bos = new BufferOpOS();
		bos.out = 1;
		BufferOpIS bis = new BufferOpIS();

		InOutExecution ex = e.inOutOp(new QName(OPERATION_NAMESPACE, "BufferOperation"));
		assertNotNull(ex);
		ex.pipeIn(e.newOutput(bos));
		ex.pipeOut(e.newInput(bis));
		e.submit();
		ExecutionUnitState state = e.state(3000, TimeUnit.MILLISECONDS, ExecutionUnitState.COMPLETE);
		assertEquals(ExecutionUnitState.COMPLETE, state);
		assertEquals(1, bis.in);

	}

	public static class TestParam {

	}

	public static class TestEnv {

	}

	public static class TestInject {
		final int num;

		public TestInject(int num) {
			this.num = num;
		}
	}

	@Qualifier
	@Retention(RUNTIME)
	@Target({ FIELD, PARAMETER })
	public @interface TestQualifier {

	}

	public static class BufferOpIS implements InBuffer {

		public int in;

	}

	public static class BufferOpOS implements OutBuffer {

		public int out;

	}

	//TODO not sure why fully qualified name is needed here but won't compile without it. Will work if class is extracted to new file
	@org.apache.ode.spi.work.Operation.OperationSet(namespace = OPERATION_NAMESPACE)
	@org.apache.ode.spi.work.Command.CommandSetRef(TestCommandSet.class)
	public static class TestOperationSet {
		public static final String OPERATION_NAMESPACE = "http://ode.apache.org/operations/test";

		@Operation(name = "ParameterOperation")
		public static void paramOp(@I int[] in, @O int[] out) throws ExecutionUnitException {
			assertNotNull(in);
			assertEquals(1, in.length);
			assertNotNull(out);
			assertEquals(1, out.length);
			out[0] = in[0];
		}

		@Operation(name = "InOutOperation")
		public static void inoutOp(@IO int[] inout) throws ExecutionUnitException {
			assertNotNull(inout);
			assertEquals(1, inout.length);
			inout[0] = 2;
		}

		@Operation(name = "ReturnOperation")
		public static int returnOp(@I int in) throws ExecutionUnitException {
			assertEquals(1, in);
			return in + 1;
		}

		@Operation(name = "BufferOperation")
		public static void buffOp(BufferOpIS in, BufferOpOS out) throws ExecutionUnitException {
			assertNotNull(out.out);
			out.out = in.in;
		}

		@Operation(name = "InjectOperation")
		public static void injectOp(@IP TestInject inject) {
			assertNotNull(inject);
			assertEquals(1, inject.num);

		}

		@Operation(name = "InjectQualifierOperation")
		public static void injectQualifierOp(@IP @TestQualifier TestInject inject) {
			assertNotNull(inject);
			assertEquals(2, inject.num);

		}

		@Operation(name = "ArrayOperation")
		public static void arrayOp(@I Object[] in, @O Object[] out) {

		}

		@Operation(name = "CommandOperation")
		public static void cmdOp(BufferOpOS out, BufferOpIS in) {
			assertNotNull(out.out);
			in.in = out.out;
		}

		/*
		//@Operation(name = "OperationWithResult")
		public static void operationWithResult() {

		}

		//@Operation(name = "OperationWithParam")
		public static void operationWithParam(TestParam param) {

		}

		//@Operation(name = "OperationSequence1")
		public static void operationSequence1() {

		}

		//@Operation(name = "OperationSequence2")
		public static void operationSequence2() {

		}

		@Operation(name = "OperationParallel1")
		public static void operationParallel1() {

		}

		
		@Operation(name = "OperationEnvironment")
		public static void operationEnvironment(@Env("test") TestEnv env) {

		}

		@Operation(command = @Command(name = "CreateContext"))
		public static void createContext(ExecutionUnit unit) {

		}

		@Operation(name = "CreateContext")
		public static void pipe() {

		}*/
	}

	@org.apache.ode.spi.work.Command.CommandSet(namespace = COMMAND_NAMESPACE)
	public static interface TestCommandSet {
		public static final String COMMAND_NAMESPACE = "http://ode.apache.org/command/test";

		@Command(name = "BufferCommand")
		public void operation(BufferOpOS out, BufferOpIS in);

	}

}
