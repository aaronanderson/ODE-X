package org.apache.ode.runtime.memory.work;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.ode.runtime.memory.work.EnvironmentAction.EnvMode;
import org.apache.ode.runtime.memory.work.ExecutionBase.Mode;
import org.apache.ode.spi.work.ExecutionUnit;

public class ExecutionUnitBase implements ExecutionUnit {

	protected ExecutionUnitBase parent;
	protected Scheduler scheduler;
	protected Queue<ExecutionType> executionQueue = new LinkedList<>();
	protected Set<? super InStream> inStreams = new HashSet<>();
	protected Set<? super OutStream> outStreams = new HashSet<>();
	protected Map<QName, ?> environment = new HashMap<>();
	protected Map<? extends Throwable, ? super ExecutionBase> handlers = new HashMap<>();

	public ExecutionUnitBase(ExecutionUnitBase parent, Scheduler scheduler) {
		this.parent = parent;
		this.scheduler = scheduler;
	}

	@Override
	public ParallelExecutionUnit parallel() {
		ParallelImpl pimpl = new ParallelImpl(this, scheduler);
		executionQueue.offer(pimpl);
		return pimpl;
	}

	@Override
	public SequentialExecutionUnit sequential() {
		SequenceImpl simpl = new SequenceImpl(this, scheduler);
		executionQueue.offer(simpl);
		return simpl;
	}

	@Override
	public Execution jobCmd(QName commandName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InExecution inCmd(QName commandName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OutExecution outCmd(QName commandName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InOutExecution inOutCmd(QName commandName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Execution jobOp(QName operationName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InExecution inOp(QName operationName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OutExecution outOp(QName operationName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InOutExecution inOutOp(QName operationName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Execution run(Job job) {
		InstanceExec ie = new InstanceExec(this, Mode.JOB, job);
		executionQueue.offer(ie);
		return ie;
	}

	@Override
	public InExecution run(In<?> in) {
		InstanceExec ie = new InstanceExec(this, Mode.IN, in);
		executionQueue.offer(ie);
		return ie;
	}

	@Override
	public OutExecution run(Out<?> out) {
		InstanceExec ie = new InstanceExec(this, Mode.OUT, out);
		executionQueue.offer(ie);
		return ie;
	}

	@Override
	public InOutExecution run(InOut<?, ?> inout) {
		InstanceExec ie = new InstanceExec(this, Mode.INOUT, inout);
		executionQueue.offer(ie);
		return ie;
	}

	@Override
	public <I extends InStream> I inStream(Class<I> struct) throws ExecutionUnitException {
		try {
			I ins = struct.newInstance();
			inStreams.add(ins);
			return ins;
		} catch (InstantiationException | IllegalAccessException e) {
			throw new ExecutionUnitException(e);
		}

	}

	@Override
	public <O extends OutStream> O outStream(Class<O> struct) throws ExecutionUnitException {
		try {
			O os = struct.newInstance();
			outStreams.add(os);
			return os;
		} catch (InstantiationException | IllegalAccessException e) {
			throw new ExecutionUnitException(e);
		}
	}

	@Override
	public <V> ExecutionUnit setEnvironment(QName name, V value) {
		EnvironmentAction<V> ea = new EnvironmentAction(EnvMode.SET, name, value);
		executionQueue.offer(ea);
		return this;
	}

	@Override
	public <V> V getEnvironment(QName name) {
		return (V) environment.get(name);
	}

	@Override
	public ExecutionUnit unsetEnvironment(QName name) {
		EnvironmentAction<?> ea = new EnvironmentAction(EnvMode.UNSET, name, null);
		executionQueue.offer(ea);
		return this;
	}

	@Override
	public <E extends Throwable> void handle(E e, QName handlerOperationName) {
		//handlers.put(e, value)

	}

	@Override
	public <E extends Throwable> void handle(E e, Aborted aborted) {

	}

	static interface ExecutionType {

	}

}
