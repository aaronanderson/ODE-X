package org.apache.ode.runtime.core.work;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.namespace.QName;

//import org.apache.ode.runtime.core.work.ExecutionUnitBuilder.EnvironmentAction.EnvMode;
import org.apache.ode.runtime.core.work.ExecutionUnitBuilder.Frame;
import org.apache.ode.spi.di.DIContainer;
import org.apache.ode.spi.di.OperationAnnotationScanner.OperationModel;
import org.apache.ode.spi.work.ExecutionUnit;

public abstract class ExecutionUnitBuilder<F extends Frame> implements ExecutionUnit {

	final protected F frame;

	final protected Queue<ExecutionStage> executionBuildQueue = new LinkedList<>();

	//contextual to specific ExecutionUnit
	public static class Frame {

		final protected Frame parentFrame;
		final protected WorkContext workCtx;

		//final protected Map<QName, EnvironmentAction<?>> environment = new HashMap<>();
		final protected Map<Class<? extends Throwable>, ? extends HandlerExecution> handlers = new HashMap<>();

		public Frame(Frame parentFrame) {
			this.parentFrame = parentFrame;
			this.workCtx = parentFrame.workCtx;
		}

		public Frame(WorkContext workCtx) {
			this.parentFrame = null;
			this.workCtx = workCtx;
		}
	}

	public static class WorkContext {
		final protected AtomicReference<ExecutionUnitState> execState = new AtomicReference<>(ExecutionUnitState.BUILD);
		final public AtomicBoolean hasCancels = new AtomicBoolean(false);
		final protected AtomicInteger executionCount = new AtomicInteger();
		final protected Queue<ExecutionStage> executionQueue = new ConcurrentLinkedQueue<>();
		final protected ReentrantLock stateLock = new ReentrantLock();
		final protected Condition changeState = stateLock.newCondition();
		final protected Map<QName, OperationModel> operations;
		final protected WorkScheduler scheduler;
		final protected DIContainer dic;

		public WorkContext(WorkScheduler scheduler, DIContainer dic, Map<QName, OperationModel> operations) {
			this.scheduler = scheduler;
			this.dic = dic;
			this.operations = operations;
		}

		final public boolean stateChange(ExecutionUnitState currentState, ExecutionUnitState newState) {
			stateLock.lock();
			try {
				if (execState.compareAndSet(currentState, newState)) {
					changeState.signalAll();
					return true;
				}
				return false;
			} finally {
				stateLock.unlock();
			}
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
		OperationModel model = frame.workCtx.operations.get(operationName);
		if (model == null) {
			throw new ExecutionUnitException(String.format("Unknown operation %s", operationName));
		}
		if (model.inputCount() != 0 || model.outputCount() != 0) {
			throw new ExecutionUnitException(String.format("Not Job operation %s", operationName));
		}
		OperationExec oe = new OperationExec(frame, model);
		executionBuildQueue.offer(oe);
		return oe;
	}

	@Override
	public InExecution inOp(QName operationName) throws ExecutionUnitException {
		OperationModel model = frame.workCtx.operations.get(operationName);
		if (model == null) {
			throw new ExecutionUnitException(String.format("Unknown operation %s", operationName));
		}
		if (model.inputCount() == 0 || model.outputCount() > 0) {
			throw new ExecutionUnitException(String.format("Not Out operation %s", operationName));
		}
		OperationExec oe = new OperationExec(frame, model);
		executionBuildQueue.offer(oe);
		return oe;
	}

	@Override
	public OutExecution outOp(QName operationName) throws ExecutionUnitException {
		OperationModel model = frame.workCtx.operations.get(operationName);
		if (model == null) {
			throw new ExecutionUnitException(String.format("Unknown operation %s", operationName));
		}
		if (model.outputCount() == 0 || model.inputCount() > 0) {
			throw new ExecutionUnitException(String.format("Not Out operation %s", operationName));
		}
		OperationExec oe = new OperationExec(frame, model);
		executionBuildQueue.offer(oe);
		return oe;
	}

	@Override
	public InOutExecution inOutOp(QName operationName) throws ExecutionUnitException {
		OperationModel model = frame.workCtx.operations.get(operationName);
		if (model == null) {
			throw new ExecutionUnitException(String.format("Unknown operation %s", operationName));
		}
		if (model.inputCount() == 0 || model.outputCount() == 0) {
			throw new ExecutionUnitException(String.format("Not InOut operation %s", operationName));
		}
		OperationExec oe = new OperationExec(frame, model);
		executionBuildQueue.offer(oe);
		return oe;
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
	public <I extends InBuffer> BufferInput<I> newInput(I struct) throws ExecutionUnitException {
		return (BufferInput<I>) new InBufferStage(struct);
	}

	@Override
	public <O extends OutBuffer> BufferOutput<O> newOutput(O struct) throws ExecutionUnitException {
		return (BufferOutput<O>) new OutBufferStage(struct);
	}

	@Override
	public ArrayInput newInput(Object... input) throws ExecutionUnitException {
		return new InArrayStage(input);
	}

	@Override
	public ArrayOutput newOutput(Object... output) throws ExecutionUnitException {
		return new OutArrayStage(output);
	}

	/*
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

	}*/

	/*@Override
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
	}*/

	@Override
	public <E extends Throwable> void handle(Class<E> e, QName handlerOperationName) throws ExecutionUnitException {
		//handlers.put(e, value)

	}

	@Override
	public <E extends Throwable> void handle(Class<E> e, Aborted aborted) throws ExecutionUnitException {

	}

}
