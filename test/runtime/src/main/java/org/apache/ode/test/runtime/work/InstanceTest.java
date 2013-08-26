package org.apache.ode.test.runtime.work;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Provider;

import org.apache.ode.spi.di.DIContainer.TypeLiteral;
import org.apache.ode.spi.runtime.Node;
import org.apache.ode.spi.work.ExecutionUnit.Execution;
import org.apache.ode.spi.work.ExecutionUnit.ExecutionUnitException;
import org.apache.ode.spi.work.ExecutionUnit.ExecutionUnitState;
import org.apache.ode.spi.work.ExecutionUnit.In;
import org.apache.ode.spi.work.ExecutionUnit.InBuffer;
import org.apache.ode.spi.work.ExecutionUnit.InExecution;
import org.apache.ode.spi.work.ExecutionUnit.InOut;
import org.apache.ode.spi.work.ExecutionUnit.InOutExecution;
import org.apache.ode.spi.work.ExecutionUnit.Job;
import org.apache.ode.spi.work.ExecutionUnit.Out;
import org.apache.ode.spi.work.ExecutionUnit.OutBuffer;
import org.apache.ode.spi.work.ExecutionUnit.OutExecution;
import org.apache.ode.spi.work.ExecutionUnit.Work;
import org.apache.ode.spi.work.ExecutionUnit.WorkItem;
import org.apache.ode.test.core.TestDIContainer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class InstanceTest {

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

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		node.offline();
	}

	@Test
	public void testJob() throws Exception {
		Work e = workProvider.get();
		JobTest jt = new JobTest();
		assertFalse(jt.ran);
		Execution ex = e.run(jt);
		e.submit();
		ExecutionUnitState state = e.state(3000, TimeUnit.MILLISECONDS, ExecutionUnitState.COMPLETE);
		assertEquals(ExecutionUnitState.COMPLETE, state);
		assertTrue(jt.ran);

	}

	@Test
	public void testIn() throws Exception {
		Work e = workProvider.get();
		StringOS sos = e.newOutBuffer(StringOS.class);
		sos.out = "BeginMiddle";
		InExecution ie = e.run(new InTest());
		ie.pipeIn(sos);
		e.submit();
		e.state(3000, TimeUnit.MILLISECONDS, ExecutionUnitState.COMPLETE);
	}

	@Test
	public void testOut() throws Exception {
		Work e = workProvider.get();
		StringIS sis = e.newInBuffer(StringIS.class);

		OutExecution ioe = e.run(new OutTest());
		ioe.pipeOut(sis);
		e.submit();
		e.state(3000, TimeUnit.MILLISECONDS, ExecutionUnitState.COMPLETE);
		assertEquals("Begin", sis.in);

	}

	@Test
	public void testInOut() throws Exception {
		Work e = workProvider.get();
		StringOS sos = e.newOutBuffer(StringOS.class);
		sos.out = "First";
		StringIS sis = e.newInBuffer(StringIS.class);

		InOutExecution ioe = e.run(new InOutTest());
		ioe.pipeIn(sos);
		ioe.pipeOut(sis);
		e.submit();
		e.state(3000, TimeUnit.MILLISECONDS, ExecutionUnitState.COMPLETE);
		assertEquals("FirstMiddleLast", sis.in + "Last");

	}

	@Test
	public void testChain() throws Exception {
		Work e = workProvider.get();
		StringOS sos = e.newOutBuffer(StringOS.class);
		sos.out = "First";

		OutExecution oe = e.run(new ChainOutTest());
		InOutExecution ioe = e.run(new ChainInOutTest());
		oe.pipeOut(ioe);
		InExecution ie = e.run(new ChainInTest());
		ie.pipeIn(ioe);
		e.submit();
		e.state(3000, TimeUnit.MILLISECONDS, ExecutionUnitState.COMPLETE);

	}

	@Test
	public void testSequence() throws Exception {
		Work e = workProvider.get();
		IntIS iis = e.newInBuffer(IntIS.class);
		AtomicInteger integer = new AtomicInteger(1);
		OutExecution oeu1 = e.run(new SeqOutTest(1, integer));
		OutExecution oeu2 = e.run(new SeqOutTest(2, integer));
		e.sequential(oeu1, oeu2);
		oeu2.pipeOut(iis);
		e.submit();
		e.state(300000, TimeUnit.MILLISECONDS, ExecutionUnitState.COMPLETE);
		assertEquals(3, iis.in);
	}

	//@Test
	public void testParallel() throws Exception {
		Work e = workProvider.get();
		CountDownLatch cdl = new CountDownLatch(2);
		//OutExecution oeu1 = e.run(new OutParallel(1, cdl));
		//OutExecution oeu2 = e.run(new OutParallel(2, cdl));
		e.submit();
	}

	//@Test
	public void testOneToMany() throws Exception {
		Work e = workProvider.get();
		//OutExecution oeu1 = e.run(new OutSharedParallel(1));
		//OutExecution oeu2 = e.run(new OutSharedParallel(2));
		//InExecution ie = e.run(new InSharedParallel());
		//oeu1.pipeOut(ie);
		//oeu2.pipeOut(ie);
		e.submit();
	}

	//@Test
	public void testManyToOne() throws Exception {
		Work e = workProvider.get();
		//OutExecution oeu1 = e.run(new OutSharedParallel(1));
		//OutExecution oeu2 = e.run(new OutSharedParallel(2));
		//InExecution ie = e.run(new InSharedParallel());
		//oeu1.pipeOut(ie);
		//oeu2.pipeOut(ie);
		//e.submit();
	}

	public static class JobTest implements Job {
		public boolean ran = false;

		@Override
		public void run(WorkItem execUnit) {
			ran = true;
			//System.out.println("******************************* JobTest ran");
		}

	}

	public static class StringIS implements InBuffer {
		public String in;
	}

	public static class StringOS implements OutBuffer {
		public String out;
	}

	public static class IntIS implements InBuffer {
		public int in;
	}

	public static class IntOS implements OutBuffer {
		public int out;
	}

	public static class IntQIS implements InBuffer {
		public Queue<Integer> in;
	}

	public static class InTest implements In<StringIS> {

		@Override
		public void in(WorkItem execUnit, StringIS in) {
			String msg = in.in;
			assertNotNull(msg);
			assertEquals("BeginMiddleEnd", msg.concat("End"));
		}

	}

	public static class OutTest implements Out<StringOS> {

		@Override
		public void out(WorkItem execUnit, StringOS out) {
			out.out = "Begin";
		}

	}

	public static class InOutTest implements InOut<StringIS, StringOS> {

		@Override
		public void inOut(WorkItem execUnit, StringIS in, StringOS out) {
			String msg = in.in;
			assertNotNull(msg);
			out.out = msg.concat("Middle");
		}

	}

	public static class ChainInTest implements In<StringIS> {

		@Override
		public void in(WorkItem execUnit, StringIS in) {
			String msg = in.in;
			assertNotNull(msg);
			assertEquals("BeginMiddleEnd", msg.concat("End"));
		}

	}

	public static class ChainOutTest implements Out<StringOS> {

		@Override
		public void out(WorkItem execUnit, StringOS out) {
			out.out = "Begin";
		}

	}

	public static class ChainInOutTest implements InOut<StringIS, StringOS> {

		@Override
		public void inOut(WorkItem execUnit, StringIS in, StringOS out) {
			String msg = in.in;
			assertNotNull(msg);
			out.out = msg.concat("Middle");
		}

	}

	public static class SeqOutTest implements Out<IntOS> {

		int expected;
		AtomicInteger integer;

		public SeqOutTest(int expected, AtomicInteger integer) {
			this.expected = expected;
			this.integer = integer;
		}

		@Override
		public void out(WorkItem execUnit, IntOS out) {
			assertEquals(expected, integer.get());
			out.out = integer.incrementAndGet();
		}

	}

}
