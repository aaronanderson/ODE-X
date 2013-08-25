package org.apache.ode.test.runtime.work;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

	//@Test
	public void testSingle() throws Exception {
		Work e = workProvider.get();
		e.run(new OutEcho()).pipeOut(e.run(new InOutEcho())).pipeOut(e.run(new InEcho()));
		e.submit();

		e = workProvider.get();
		try {
			e.run(new OutEcho()).pipeOut(e.run(new InOutEcho())).pipeOut(e.run(new InEcho()));
			e.run(new OutEcho());
			e.submit();
			fail();
		} catch (ExecutionUnitException ee) {

		}

	}

	@Test
	public void testBuffer() throws Exception {
		Work e = workProvider.get();
		StringOS sos = e.newOutBuffer(StringOS.class);
		sos.out = "First";
		StringIS sis = e.newInBuffer(StringIS.class);

		InOutExecution ioe = e.run(new InOutEcho());
		ioe.pipeIn(sos);
		ioe.pipeOut(sis);
		e.submit();
		e.state(30000, TimeUnit.MILLISECONDS, ExecutionUnitState.COMPLETE);
		assertEquals("FirstMiddleLast", sis.in + "Last");

	}

	//@Test
	public void testSequence() throws Exception {
		Work e = workProvider.get();
		e.beginSequential();
		OutExecution oeu1 = e.run(new OutSequence(1));
		OutExecution oeu2 = e.run(new OutSequence(2));
		InOutExecution ioe = e.run(new InOutSequence());
		oeu1.pipeOut(ioe);
		oeu2.pipeOut(ioe);
		e.endSequential();
		StringBuilder sb = new StringBuilder();
		ioe.pipeOut(e.run(new InSequence(sb)));
		e.submit();
	}

	//@Test
	public void testParallel() throws Exception {
		Work e = workProvider.get();
		e.beginParallel();
		CountDownLatch cdl = new CountDownLatch(2);
		OutExecution oeu1 = e.run(new OutParallel(1, cdl));
		OutExecution oeu2 = e.run(new OutParallel(2, cdl));
		InOutExecution ioe = e.run(new InOutParallel());
		oeu1.pipeOut(ioe);
		oeu2.pipeOut(ioe);
		e.endParallel();
		StringBuilder sb = new StringBuilder();
		ioe.pipeOut(e.run(new InParallel(sb)));
		e.submit();
	}

	//@Test
	public void testSharedParallel() throws Exception {
		Work e = workProvider.get();
		e.beginParallel();
		OutExecution oeu1 = e.run(new OutSharedParallel(1));
		OutExecution oeu2 = e.run(new OutSharedParallel(2));
		InExecution ie = e.run(new InSharedParallel());
		oeu1.pipeOut(ie);
		oeu2.pipeOut(ie);
		e.endParallel();
		e.submit();
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

	public static class OutEcho implements Out<StringOS> {

		@Override
		public void out(WorkItem execUnit, StringOS out) {
			out.out = "Begin";
		}

	}

	public static class InOutEcho implements InOut<StringIS, StringOS> {

		@Override
		public void inOut(WorkItem execUnit, StringIS in, StringOS out) {
			String msg = in.in;
			assertNotNull(msg);
			out.out = msg.concat("Middle");
		}

	}

	public static class OutSequence implements Out<IntOS> {

		int num = 0;

		public OutSequence(int num) {
			this.num = num;
		}

		@Override
		public void out(WorkItem execUnit, IntOS out) {
			out.out = num;
		}

	}

	public static class InOutSequence implements InOut<IntQIS, StringOS> {

		@Override
		public void inOut(WorkItem execUnit, IntQIS in, StringOS out) {
			Integer int1 = in.in.poll();
			assertNotNull(int1);
			Integer int2 = in.in.poll();
			assertNotNull(int2);
			out.out = String.format("Seq %d %d", int1, int2);
		}

	}

	public static class InSequence implements In<IntIS> {

		StringBuilder sb;

		public InSequence(StringBuilder sb) {
			this.sb = sb;
		}

		@Override
		public void in(WorkItem execUnit, IntIS in) {
			sb.append(in.in);
		}

	}

	public static class OutParallel implements Out<IntOS> {

		CountDownLatch cdl;
		int num = 0;

		public OutParallel(int num, CountDownLatch cdl) {
			this.num = num;
			this.cdl = cdl;
		}

		@Override
		public void out(WorkItem execUnit, IntOS out) {
			try {
				assertTrue(cdl.await(3000, TimeUnit.MILLISECONDS));
			} catch (InterruptedException ie) {
				fail();
			}
			out.out = num;
		}

	}

	public static class InOutParallel implements InOut<IntQIS, StringOS> {

		@Override
		public void inOut(WorkItem execUnit, IntQIS in, StringOS out) {
			Integer int1 = in.in.poll();
			assertNotNull(int1);
			Integer int2 = in.in.poll();
			assertNotNull(int2);
			out.out = String.format("Para %d %d", int1, int2);
		}

	}

	public static class InParallel implements In<IntIS> {

		StringBuilder sb;

		public InParallel(StringBuilder sb) {
			this.sb = sb;
		}

		@Override
		public void in(WorkItem execUnit, IntIS in) {
			sb.append(in.in);
		}

	}

	public static class OutSharedParallel implements Out<IntOS> {

		int num = 0;

		public OutSharedParallel(int num) {
			this.num = num;
		}

		@Override
		public void out(WorkItem execUnit, IntOS out) {
			out.out = num;
		}

	}

	public static class InSharedParallel implements In<IntQIS> {

		@Override
		public void in(WorkItem execUnit, IntQIS in) {
			Integer int1 = in.in.poll();
			assertNotNull(int1);
			Integer int2 = in.in.poll();
			assertNotNull(int2);
		}

	}

	public static class InEcho implements In<StringIS> {

		@Override
		public void in(WorkItem execUnit, StringIS in) {
			String msg = in.in;
			assertNotNull(msg);
			assertEquals("BeginMiddleEnd", msg.concat("End"));
		}

	}

}
