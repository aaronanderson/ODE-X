package org.apache.ode.runtime.memory.operation;

import javax.xml.namespace.QName;

import org.apache.ode.spi.work.ExecutionUnit;

public class ExecutionUnitBase implements ExecutionUnit {

	Scheduler scheduler;

	public ExecutionUnitBase(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	@Override
	public ParallelExecutionUnit parallel() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SequentialExecutionUnit sequential() {
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InExecution run(In<?> in) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OutExecution run(Out<?> out) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InOutExecution run(InOut<?, ?> inout) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <I extends InStream> I inStream(Class<I> struct) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <O extends OutStream> O outStream(Class<O> struct) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <V> ExecutionUnit setEnvironment(QName name, V value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <V> V getEnvironment(QName name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ExecutionUnit unsetEnvironment(QName name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <E extends Throwable> void handle(E e, QName handlerOperationName) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public <E extends Throwable> void handle(E e, Aborted aborted) {
		// TODO Auto-generated method stub
		
	}

	

}
