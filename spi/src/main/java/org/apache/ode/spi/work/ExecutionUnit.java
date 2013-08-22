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

	public ExecutionUnit beginParallel() throws ExecutionUnitException;

	public ExecutionUnit endParallel() throws ExecutionUnitException;

	public ExecutionUnit beginSequential() throws ExecutionUnitException;

	public ExecutionUnit endSequential() throws ExecutionUnitException;

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

	public <I extends Buffer> I newBuffer(Class<I> struct) throws ExecutionUnitException;

	public <V> ExecutionUnit setEnvironment(QName name, V value) throws ExecutionUnitException;;

	public <V> V getEnvironment(QName name) throws ExecutionUnitException;;

	public ExecutionUnit unsetEnvironment(QName name) throws ExecutionUnitException;;

	public <E extends Throwable> void handle(Class<E> e, QName handlerOperationName) throws ExecutionUnitException;;

	public <E extends Throwable> void handle(Class<E> e, Aborted aborted) throws ExecutionUnitException;

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
		READY, BLOCK_IN, RUN, BLOCK_RUN, BLOCK_OUT, CANCEL, ABORT, COMPLETE;
	}

	public static interface Execution {

		public void initializer();

		public void finalizer();

		public ExecutionState state() throws ExecutionUnitException;

	}

	public static interface InExecution {

		public <O extends Buffer> InExecution pipeIn(O buffer, Transform... transforms) throws ExecutionUnitException;

		public OutExecution pipeIn(OutExecution execUnit, Transform... transforms) throws ExecutionUnitException;

		public InOutExecution pipeIn(InOutExecution execUnit, Transform... transforms) throws ExecutionUnitException;

		public ExecutionState state() throws ExecutionUnitException;

	}

	public static interface OutExecution {

		public <I extends Buffer> OutExecution pipeOut(I buffer, Transform... transforms) throws ExecutionUnitException;

		public InExecution pipeOut(InExecution execUnit, Transform... transforms) throws ExecutionUnitException;

		public InOutExecution pipeOut(InOutExecution execUnit, Transform... transforms) throws ExecutionUnitException;

		public ExecutionState state() throws ExecutionUnitException;
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

		public <I extends Buffer> I inBuffer();

		public <O extends Buffer> O outBuffer();

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

	//Using a buffer pattern data is passed by order in the buffer
	//implements InStream or OutStream
	//no methods, only fields

	//read data in
	public static interface Buffer {

	}

	//While operations are static and modeled at DI discovery time  these interfaces allow any instance to participate in execution. 

	public static interface Job {

		public void run(WorkItem execUnit);

	}

	public static interface In<I extends Buffer> {

		public void in(WorkItem execUnit, I in);
	}

	public static interface Out<O extends Buffer> {

		public void out(WorkItem execUnit, O out);
	}

	public static interface InOut<I extends Buffer, O extends Buffer> {

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

	@Retention(RUNTIME)
	@Target(PARAMETER)
	public @interface I {
		int size() default 0;
	}

	@Retention(RUNTIME)
	@Target(PARAMETER)
	public @interface O {
		int size() default 0;
	}

}
