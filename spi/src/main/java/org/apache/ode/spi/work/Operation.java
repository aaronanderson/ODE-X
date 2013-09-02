package org.apache.ode.spi.work;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(METHOD)
public @interface Operation {
	//static methods that can be represented using MethodHandlers. This should make them more compatible with lambda expression when they become available with JDK 8
	//Operations are discovered during dependency injection and static models are generated for them.

	//Unit of work. always operate in the context of an ExecutionUnit. corespond one to one with event execution. Different kinds of operations, execinstruction, event, platform ops like load store, etc
	//Flow:
	//operation is executed. Resulting operations may be a result are queued up. each operation can by sync or async.
	//scheduler polls operation. as result operations are completed the next sync ops can be submitted to the scheduler. Once all resulting operations are complete then the operation itself is marked complete
	//operations shift between scheduler, PU, and IO. 

	/* @OperationSet("somenamespace")
	  public class 
	  
	  @Operation("init")
	  public void init (UNIT unit)
	  MyOp my = new MyOp();
	  Unit.execute("init2", myOp);
	 
	

	//can be polled by any of the three schedulers

	public static enum Status {
		BLOCK, READY, RUN, COMPLETE, CANCEL, ABORT;

	}

	public static enum Type {
		EVENT, IO, PROCESS;
	}

	public static class State {
		public Status status;
		public Type type;
	}

	 */

	String namespace() default "";

	String name() default "";

	//https://blogs.oracle.com/toddfast/entry/creating_nested_complex_java_annotations
	Command command() default @Command(name = "");

	@Retention(RUNTIME)
	@Target(TYPE)
	public @interface OperationSet {
		public String namespace() default "";

		public String commandNamespace() default "";
	}

	@Retention(RUNTIME)
	@Target(METHOD)
	public @interface Command {

		String namespace() default "";

		String name();

	}

	//since java parameter names are lost at compile time a mechanism for named lookups is required. Using the stream pattern data is passed by position
	//Operation stream order is determined by parameter order and return order. However, if a parameter matches one of the following criteria the contextual
	//value is placed instead of a value from the stream. The same is true for return types

	//non stream value parameters
	//InStream
	//OutStream
	//ExecutionUnit
	//parameters annotated with Env
	//parameters annotated @Inject

	//valid stream return values
	//void (required if OutStream passed in)
	//Object []
	//List<?>
	//Object (single value)

	/*@Retention(RUNTIME)
	@Target(PARAMETER)
	public @interface Env {

		public String value();
	}*/

	@Retention(RUNTIME)
	@Target(PARAMETER)
	public @interface I {

	}

	@Retention(RUNTIME)
	@Target(PARAMETER)
	public @interface O {

	}

	@Retention(RUNTIME)
	@Target(PARAMETER)
	public @interface IO {

	}

	//Provided. In addition to annotation below if a qualifier annotation is present it will be used for DI lookup
	@Retention(RUNTIME)
	@Target(PARAMETER)
	public @interface IP {
	}

	@Retention(RUNTIME)
	@Target(PARAMETER)
	public @interface OP {

	}

	@Retention(RUNTIME)
	@Target(PARAMETER)
	public @interface IOP {

	}

}
