package org.apache.ode.test.runtime.work;

import static org.apache.ode.test.runtime.work.OperationTest.TestCommandSet.COMMAND_NAMESPACE;
import static org.apache.ode.test.runtime.work.OperationTest.TestOperationSet.OPERATION_NAMESPACE;
import static org.junit.Assert.assertNotNull;

import javax.inject.Provider;

import org.apache.ode.spi.di.DIContainer.TypeLiteral;
import org.apache.ode.spi.work.Command;
import org.apache.ode.spi.work.ExecutionUnit;
import org.apache.ode.spi.work.ExecutionUnit.ExecutionUnitException;
import org.apache.ode.spi.work.ExecutionUnit.H;
import org.apache.ode.spi.work.ExecutionUnit.I;
import org.apache.ode.spi.work.ExecutionUnit.InStream;
import org.apache.ode.spi.work.ExecutionUnit.O;
import org.apache.ode.spi.work.ExecutionUnit.OutStream;
import org.apache.ode.spi.work.Operation;
import org.apache.ode.test.core.TestDIContainer;
import org.apache.ode.test.runtime.work.OperationTest.TestCommandSet.StreamCmd;
import org.junit.BeforeClass;
import org.junit.Test;

public class OperationTest {

	static Provider<ExecutionUnit> execProvider;

	//static TestOperationInstanceSet operationInstance;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		TestDIContainer container = TestDIContainer.CONTAINER.get();
		assertNotNull(container);
		execProvider = container.getInstance(new TypeLiteral<Provider<ExecutionUnit>>() {
		});
		assertNotNull(execProvider);
		//operationInstance = container.getInstance(TestOperationInstanceSet.class);
		//assertNotNull(operationInstance);

	}

	@Test
	public void testOperation() throws Exception {
		ExecutionUnit eu = execProvider.get();
		//eu.run(new OutEcho()).pipeOut(eu.run(new InOutEcho())).pipeOut(eu.run(new InEcho()));
		//eu.runOperation(new QName(OPERATION_NAMESPACE, "TestOperation"));
		//eu.submit();
		//eu.runOperation(operationInstance, new QName(OPERATION_NAMESPACE, "OperationInstance"));
		//eu.run(new TestInOut1()).pipeOut(eu.run(new TestInOut2()));

		//eu.runCommand(new QName(OPERATION_NAMESPACE, "OperationWithResult")).pipeOut(eu.runOperation());

	}

	public static class TestParam {

	}

	public static class TestEnv {

	}

	public static class StreamOpIS implements InStream {

		public int in;

	}

	public static class StreamOpOS implements OutStream {

		public int out;

	}

	//TODO not sure why fully qualified name is needed here but won't compile without it. Will work if class is extracted to new file
	@org.apache.ode.spi.work.Operation.OperationSet(namespace = OPERATION_NAMESPACE)
	public static class TestOperationSet {
		public static final String OPERATION_NAMESPACE = "http://ode.apache.org/operations/test";

		@Operation(name = "StreamOperation")
		public static class StreamOp {
			public void operation(StreamOpOS out, StreamOpIS in) throws ExecutionUnitException {
				assertNotNull(out.out);
				in.in = out.out;
			}
		}

		@Operation(name = "PrimitiveOperation")
		public static class PrimitiveOp {
			public int operation(int in) {
				return in;
			}

		}

		@Operation(name = "HolderOperation")
		public static class HolderOp {
			public void operation(@I H<Integer> in, @O H<Integer> out) {

			}

		}

		@Operation(name = "ArrayOperation")
		public static interface ArrayOp {
			public void operation(@I Object[] in, @O Object[] out);

		}

		@Operation(name = "CommandOperation")
		public static class StreamCmdOp implements StreamCmd {
			public void operation(StreamOpOS out, StreamOpIS in) {
				assertNotNull(out.out);
				in.in = out.out;
			}
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
	public static class TestCommandSet {
		public static final String COMMAND_NAMESPACE = "http://ode.apache.org/command/test";

		@Command(name = "StreamCommand")
		public static interface StreamCmd {
			public void operation(StreamOpOS out, StreamOpIS in);
		}
	}

}
