package org.apache.ode.spi.exec.platform;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(METHOD)
public @interface Operation {

	//Unit of work. always operate in the context of an executioncontext. corespond one to one with event execution. Different kinds of operations, execinstruction, event, platform ops like load store, etc
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
	public static @interface OperationSet {
		public String namespace() default "";

		public String commandNamespace() default "";
	}

	@Retention(RUNTIME)
	@Target(METHOD)
	public @interface Command {

		String namespace() default "";

		String name();

	}

	@Retention(RUNTIME)
	@Target(PARAMETER)
	public static @interface Param {

		public String value();
	}

	public static class ParamName {

		private String name;

		public ParamName(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

}
