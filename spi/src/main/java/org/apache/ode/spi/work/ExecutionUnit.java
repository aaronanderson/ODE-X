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

	public ParallelExecutionUnit parallel();

	public SequentialExecutionUnit sequential();

	public Execution jobCmd(QName commandName);

	public InExecution inCmd(QName commandName);

	public OutExecution outCmd(QName commandName);

	public InOutExecution inOutCmd(QName commandName);

	public Execution jobOp(QName operationName);

	public InExecution inOp(QName operationName);

	public OutExecution outOp(QName operationName);

	public InOutExecution inOutOp(QName operationName);

	public Execution run(Job job);

	public InExecution run(In<?> in);

	public OutExecution run(Out<?> out);

	public InOutExecution run(InOut<?, ?> inout);

	public <I extends InStream> I inStream(Class<I> struct) throws ExecutionUnitException;

	public <O extends OutStream> O outStream(Class<O> struct) throws ExecutionUnitException;

	public <V> ExecutionUnit setEnvironment(QName name, V value);

	public <V> V getEnvironment(QName name);

	public ExecutionUnit unsetEnvironment(QName name);

	public <E extends Throwable> void handle(E e, QName handlerOperationName);

	public <E extends Throwable> void handle(E e, Aborted aborted);

	public static enum ExecutionState {
		BUILD, SUBMIT, READY, RUN, BLOCK, ABORT, CANCEL, COMPLETE;
	}

	public static interface Execution {

		public ExecutionState state(long timeout, TimeUnit unit, ExecutionState... expected) throws ExecutionUnitException;

	}

	public static interface InExecution extends Execution {

		//public void finalize();

		public <O extends OutStream> InExecution pipeIn(O stream);

		public OutExecution pipeIn(OutExecution execUnit, int... map);

		public InOutExecution pipeIn(InOutExecution execUnit, int... map);

	}

	public static interface OutExecution extends Execution {

		//public void initialize();

		public <I extends InStream> OutExecution pipeOut(I stream);

		public InExecution pipeOut(InExecution execUnit, int... map);

		public InOutExecution pipeOut(InOutExecution execUnit, int... map);
	}

	public static interface InOutExecution extends InExecution, OutExecution {

	}

	public static enum ExecutionUnitState {
		BUILD, SUBMIT, RUN, BLOCK, ABORT, CANCEL, COMPLETE;
	}

	public interface Work extends ExecutionUnit {

		//termination methods
		public void submit() throws ExecutionUnitException;

		ExecutionState state(long timeout, TimeUnit unit, ExecutionUnitState... expected) throws ExecutionUnitException;

		public void cancel() throws ExecutionUnitException;

	}

	public interface WorkItem extends ExecutionUnit {
		//termination methods
		public void submit() throws ExecutionUnitException;

		public Semaphore block() throws ExecutionUnitException;

		public void abort(Throwable t) throws ExecutionUnitException;

		//public <I extends InStream> I inStream();

		//public <O extends OutStream> O outStream();

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

	public static interface SequentialExecutionUnit extends ExecutionUnit {

	}

	public static interface ParallelExecutionUnit extends ExecutionUnit {

	}

	public static interface Aborted {

		public void abort(Throwable t);

	}

	//Using a stream pattern data is passed by order in the stream
	//implements InStream or OutStream
	//no methods, only fields

	//read data in
	public static interface InStream {

	}

	//write data out
	public static interface OutStream {

	}

	//While operations are static and modeled at DI discovery time  these interfaces allow any instance to participate in execution. 

	public static interface Job {

		public void run(WorkItem execUnit);

	}

	public static interface In<I extends InStream> {

		public void in(WorkItem execUnit, I in);
	}

	public static interface Out<O extends OutStream> {

		public void out(WorkItem execUnit, O out);
	}

	public static interface InOut<I extends InStream, O extends OutStream> {

		public void inOut(WorkItem execUnit, I in, O out);
	}

	public static final class H<T> {

		public final T value;

		public H(T value) {
			this.value = value;
		}

	}

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
