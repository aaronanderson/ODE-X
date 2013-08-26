package org.apache.ode.runtime.memory.work;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Semaphore;

import javax.xml.namespace.QName;

import org.apache.ode.runtime.memory.work.ExecutionUnitBuilder.EnvironmentAction.EnvMode;
import org.apache.ode.runtime.memory.work.ExecutionUnitBuilder.Frame;
import org.apache.ode.spi.work.ExecutionUnit;

public class ExecutionUnitBuilder<F extends Frame> implements ExecutionUnit {

	final protected F frame;

	final protected Queue<ExecutionStage> executionBuildQueue = new LinkedList<>();

	public static class Frame {

		final protected Frame parentFrame;

		final protected Map<QName, EnvironmentAction<?>> environment = new HashMap<>();
		final protected Map<Class<? extends Throwable>, ? extends HandlerExecution> handlers = new HashMap<>();
		protected Map<OutBuffer, BufferStage> inBuffers = new HashMap<>();
		protected Map<InBuffer, BufferStage> outBuffers = new HashMap<>();
		protected Semaphore block;

		public Frame(Frame parentFrame) {
			this.parentFrame = parentFrame;
		}
	}

	public ExecutionUnitBuilder(F frame) {
		this.frame = frame;
	}

	@Override
	public <S extends Execution, D extends Execution> ExecutionUnit sequential(S supExecUnit, D depExecUnit) throws ExecutionUnitException {
		ExecutionStage supExecUnitStage = (ExecutionStage) supExecUnit;
		ExecutionStage depExecUnitStage = (ExecutionStage) depExecUnit;
		if (depExecUnitStage.supDependency != null) {
			throw new ExecutionUnitException("Dependency execution unit already has sequential dependency");
		}
		if (supExecUnitStage.infDependency == null) {
			supExecUnitStage.infDependency = new LinkedList<>();
		}
		supExecUnitStage.infDependency.add(depExecUnitStage);
		depExecUnitStage.supDependency = supExecUnitStage;

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
		InstanceExec ie = new InstanceExec(frame, job);
		executionBuildQueue.offer(ie);
		return ie;
	}

	@Override
	public InExecution run(In<?> in) throws ExecutionUnitException {
		InstanceExec ie = new InstanceExec(frame, in);
		executionBuildQueue.offer(ie);
		return ie;
	}

	@Override
	public OutExecution run(Out<?> out) throws ExecutionUnitException {
		InstanceExec ie = new InstanceExec(frame, out);
		executionBuildQueue.offer(ie);
		return ie;
	}

	@Override
	public InOutExecution run(InOut<?, ?> inout) throws ExecutionUnitException {
		InstanceExec ie = new InstanceExec(frame, inout);
		executionBuildQueue.offer(ie);
		return ie;
	}

	@Override
	public <I extends InBuffer> I newInBuffer(Class<I> struct) throws ExecutionUnitException {
		try {
			I ins = struct.newInstance();
			BufferStage bs = new BufferStage(ins);
			frame.outBuffers.put((InBuffer) ins, bs);
			return ins;
		} catch (InstantiationException | IllegalAccessException e) {
			throw new ExecutionUnitException(e);
		}

	}

	@Override
	public <O extends OutBuffer> O newOutBuffer(Class<O> struct) throws ExecutionUnitException {
		try {
			O ins = struct.newInstance();
			BufferStage bs = new BufferStage(ins);
			frame.inBuffers.put(ins, bs);
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
