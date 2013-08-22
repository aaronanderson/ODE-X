package org.apache.ode.runtime.memory.work;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

import javax.xml.namespace.QName;

import org.apache.ode.runtime.memory.work.ExecutionStage.Mode;
import org.apache.ode.runtime.memory.work.ExecutionUnitBuilder.EnvironmentAction.EnvMode;
import org.apache.ode.spi.work.ExecutionUnit;

public class ExecutionUnitBuilder implements ExecutionUnit {

	final protected Frame frame;

	final protected Queue<ExecutionStage> executionBuildQueue = new LinkedList<>();
	protected Queue<BuildMode> mode = new LinkedList<>();

	public static class Frame {

		final protected Frame parentFrame;
		final protected Map<QName, EnvironmentAction<?>> environment = new HashMap<>();
		final protected Map<Class<? extends Throwable>, ? extends HandlerExecution> handlers = new HashMap<>();
		protected Map<? super Buffer, BufferStage> buffers = new HashMap<>();
		protected Semaphore block;

		public Frame(Frame parentFrame) {
			this.parentFrame = parentFrame;
		}
	}

	public static class RootFrame extends Frame {

		final protected Queue<ExecutionStage> executionQueue = new ConcurrentLinkedQueue<>();

		public RootFrame() {
			super(null);
		}
	}

	public ExecutionUnitBuilder() {
		this.frame = new RootFrame();
	}

	public ExecutionUnitBuilder(Frame parentFrame) {
		this.frame = new Frame(parentFrame);
	}

	public static enum BuildMode {
		CHAIN, SEQUENTIAL, PARALLEL;
	}

	@Override
	public ExecutionUnit beginParallel() throws ExecutionUnitException {
		if (mode.peek() == BuildMode.CHAIN) {
			throw new ExecutionUnitException("Invalid combination");
		}
		mode.offer(BuildMode.PARALLEL);
		return this;
	}

	@Override
	public ExecutionUnit endParallel() throws ExecutionUnitException {
		if (mode.poll() != BuildMode.PARALLEL) {
			throw new ExecutionUnitException("Invalid combination");
		}
		return this;
	}

	@Override
	public ExecutionUnit beginSequential() throws ExecutionUnitException {
		if (mode.peek() == BuildMode.CHAIN) {
			throw new ExecutionUnitException("Invalid combination");
		}
		mode.offer(BuildMode.SEQUENTIAL);
		return this;
	}

	@Override
	public ExecutionUnit endSequential() throws ExecutionUnitException {
		if (mode.poll() != BuildMode.SEQUENTIAL) {
			throw new ExecutionUnitException("Invalid combination");
		}
		return this;
	}

	@Override
	public Execution jobCmd(QName commandName) throws ExecutionUnitException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InExecution inCmd(QName commandName) throws ExecutionUnitException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OutExecution outCmd(QName commandName) throws ExecutionUnitException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InOutExecution inOutCmd(QName commandName) throws ExecutionUnitException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Execution jobOp(QName operationName) throws ExecutionUnitException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InExecution inOp(QName operationName) throws ExecutionUnitException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OutExecution outOp(QName operationName) throws ExecutionUnitException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InOutExecution inOutOp(QName operationName) throws ExecutionUnitException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Execution run(Job job) throws ExecutionUnitException {
		InstanceExec ie = new InstanceExec(frame, Mode.JOB, job);
		executionBuildQueue.offer(ie);
		return ie;
	}

	@Override
	public InExecution run(In<?> in) throws ExecutionUnitException {
		InstanceExec ie = new InstanceExec(frame, Mode.IN, in);
		executionBuildQueue.offer(ie);
		return ie;
	}

	@Override
	public OutExecution run(Out<?> out) throws ExecutionUnitException {
		InstanceExec ie = new InstanceExec(frame, Mode.OUT, out);
		executionBuildQueue.offer(ie);
		return ie;
	}

	@Override
	public InOutExecution run(InOut<?, ?> inout) throws ExecutionUnitException {
		InstanceExec ie = new InstanceExec(frame, Mode.INOUT, inout);
		executionBuildQueue.offer(ie);
		return ie;
	}

	@Override
	public <I extends Buffer> I newBuffer(Class<I> struct) throws ExecutionUnitException {
		try {
			I ins = struct.newInstance();
			BufferStage bs = new BufferStage(ins);
			frame.buffers.put(ins, bs);
			return ins;
		} catch (InstantiationException | IllegalAccessException e) {
			throw new ExecutionUnitException(e);
		}

	}

	public static class EnvironmentAction<V> {
		final EnvMode mode;
		final QName name;
		final V value;

		EnvironmentAction(EnvMode mode, QName name, V value) {
			this.mode = mode;
			this.name = name;
			this.value = value;
		}

		public static enum EnvMode {
			SET, UNSET;
		}

	}

	@Override
	public <V> ExecutionUnit setEnvironment(QName name, V value) throws ExecutionUnitException {
		frame.environment.put(name, new EnvironmentAction(EnvMode.SET, name, value));
		return this;
	}

	@Override
	public <V> V getEnvironment(QName name) throws ExecutionUnitException {

		Frame canidate = frame;
		while (canidate != null) {
			EnvironmentAction ea = frame.environment.get(name);
			if (ea != null) {
				if (ea.mode == EnvMode.SET) {
					return (V) ea.value;
				}
				if (ea.mode != EnvMode.UNSET) {
					return null;
				}

			}
			canidate = frame.parentFrame;
		}
		return null;
	}

	@Override
	public ExecutionUnit unsetEnvironment(QName name) throws ExecutionUnitException {
		frame.environment.put(name, new EnvironmentAction(EnvMode.UNSET, name, null));
		return this;
	}

	@Override
	public <E extends Throwable> void handle(Class<E> e, QName handlerOperationName) throws ExecutionUnitException {
		//handlers.put(e, value)

	}

	@Override
	public <E extends Throwable> void handle(Class<E> e, Aborted aborted) throws ExecutionUnitException {

	}

}
