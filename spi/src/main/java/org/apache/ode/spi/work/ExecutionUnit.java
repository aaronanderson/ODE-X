package org.apache.ode.spi.work;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.xml.namespace.QName;

public interface ExecutionUnit {

	//Fluent API

	//New ExecutionUnits

	public <S extends Execution, D extends Execution> ExecutionUnit sequential(S supExecUnit, D depExecUnit) throws ExecutionUnitException;

	public Execution jobCmd(QName commandName) throws ExecutionUnitException;

	public InExecution inCmd(QName commandName) throws ExecutionUnitException;

	public OutExecution outCmd(QName commandName) throws ExecutionUnitException;

	public InOutExecution inOutCmd(QName commandName) throws ExecutionUnitException;

	public Execution jobOp(QName operationName) throws ExecutionUnitException;

	public InExecution inOp(QName operationName) throws ExecutionUnitException;

	public OutExecution outOp(QName operationName) throws ExecutionUnitException;

	public InOutExecution inOutOp(QName operationName) throws ExecutionUnitException;

	public Execution run(Job job) throws ExecutionUnitException;

	public InExecution run(In<?> in) throws ExecutionUnitException;

	public OutExecution run(Out<?> out) throws ExecutionUnitException;

	public InOutExecution run(InOut<?, ?> inout) throws ExecutionUnitException;

	public <I extends InBuffer> BufferInput<I> newInput(I struct) throws ExecutionUnitException;

	public <O extends OutBuffer> BufferOutput<O> newOutput(O struct) throws ExecutionUnitException;

	public ArrayInput newInput(Object... input) throws ExecutionUnitException;

	public ArrayOutput newOutput(Object... output) throws ExecutionUnitException;

	//public <V> ExecutionUnit setEnvironment(QName name, V value) throws ExecutionUnitException;;

	//public <V> V getEnvironment(QName name) throws ExecutionUnitException;;

	//public ExecutionUnit unsetEnvironment(QName name) throws ExecutionUnitException;;

	public <E extends Throwable> void handle(Class<E> e, QName handlerOperationName) throws ExecutionUnitException;;

	public <E extends Throwable> void handle(Class<E> e, Aborted aborted) throws ExecutionUnitException;

	//Transforms should be associative, i.e. order of the transform does not matter
	public static interface Transform {

	}

	public static interface OneToOne extends Transform {

		public void transform(Object[] from, Object[] to);
	}

	public static interface OneToMany extends Transform {

		public void transform(Object[] from, Object[][] to);
	}

	public static interface ManyToOne extends Transform {

		public void transform(Object[][] from, Object[] to);
	}

	public static enum ExecutionState {
		READY, BLOCK_IN, BLOCK_SEQ, RUN, BLOCK_RUN, BLOCK_OUT, CANCEL, ABORT, COMPLETE;
	}

	public static interface Execution {

		//public void initializer();

		//public void finalizer();

		public ExecutionState state() throws ExecutionUnitException;

	}

	public static interface InExecution extends Execution {

		public InExecution pipeIn(Output output, Transform... transforms) throws ExecutionUnitException;

		public OutExecution pipeIn(OutExecution execUnit, Transform... transforms) throws ExecutionUnitException;

		public InOutExecution pipeIn(InOutExecution execUnit, Transform... transforms) throws ExecutionUnitException;

	}

	public static interface OutExecution extends Execution {

		public OutExecution pipeOut(Input input, Transform... transforms) throws ExecutionUnitException;

		public InExecution pipeOut(InExecution execUnit, Transform... transforms) throws ExecutionUnitException;

		public InOutExecution pipeOut(InOutExecution execUnit, Transform... transforms) throws ExecutionUnitException;

	}

	public static interface InOutExecution extends InExecution, OutExecution {

	}

	public static enum ExecutionUnitState {
		BUILD, SUBMIT, READY, RUN, BLOCK, CANCEL, COMPLETE;
	}

	public interface Work extends ExecutionUnit {

		//termination methods
		public void submit() throws ExecutionUnitException;

		ExecutionUnitState state(long timeout, TimeUnit unit, ExecutionUnitState... expected) throws ExecutionUnitException;

		public void cancel() throws ExecutionUnitException;

	}

	public interface WorkItem extends ExecutionUnit {
		//termination methods
		public void submit() throws ExecutionUnitException;

		public Semaphore block() throws ExecutionUnitException;

		public void abort(Throwable t) throws ExecutionUnitException;

		public <I extends InBuffer> I inBuffer() throws ExecutionUnitException;

		public <O extends OutBuffer> O outBuffer() throws ExecutionUnitException;

	}

	public static class ExecutionUnitException extends Exception {
		public ExecutionUnitException() {
		}

		public ExecutionUnitException(String msg) {
			super(msg);
		}

		public ExecutionUnitException(Throwable t) {
			super(t);
		}

	}

	public static interface Aborted {

		public void abort(Throwable t);

	}

	public static interface Input {

	}

	public static interface Output {

	}

	//Using a buffer pattern data is passed by order in the buffer
	//implements InStream or OutStream
	//no methods, only fields

	//read data in
	public static interface InBuffer {

	}

	public static interface OutBuffer {

	}

	public static interface BufferInput<I extends InBuffer> extends Input {
		I buffer();
	}

	public static interface BufferOutput<O extends OutBuffer> extends Output {
		O buffer();
	}

	public static interface ArrayInput extends Input {
		Object[] array();
	}

	public static interface ArrayOutput extends Output {
		Object[] array();
	}

	//While operations are static and modeled at DI discovery time  these interfaces allow any instance to participate in execution. 

	public static interface Job {

		public void run(WorkItem execUnit);

	}

	public static interface In<I extends InBuffer> {

		public void in(WorkItem execUnit, I in);
	}

	public static interface Out<O extends OutBuffer> {

		public void out(WorkItem execUnit, O out);
	}

	public static interface InOut<I extends InBuffer, O extends OutBuffer> {

		public void inOut(WorkItem execUnit, I in, O out);
	}

	/*
	 zero arg constructor or single element array instead
	public static final class H<T> {

		public final T value;

		public H(T value) {
			this.value = value;
		}

	}*/

}
