package org.apache.ode.spi.work;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.apache.ode.spi.runtime.Component;

//Dispatchers match abstract commands to concrete operations
@Retention(RUNTIME)
@Target(TYPE)
public @interface Dispatcher {

	//Dispatchers can be associated with a component which determines the dispatcher chain order, from highest dependency to lowest dependency
	Component component() default @Component();

	//@BuildDispatch(@Filter(namespace="foo"))
	// <E extends Execution> E dispatch(QName command, WorkItem workItem)
	//@ExecDispatch(@Filter(namespace="foo"))
	// QName dispatch(QName command, Object[] input)

	@Retention(RUNTIME)
	@Target(METHOD)
	public static @interface BuildDispatch {
		Filter[] value() default {};

		boolean singleThread() default false;

		Component component() default @Component();
	}

	@Retention(RUNTIME)
	@Target(METHOD)
	public static @interface ExecDispatch {
		Filter[] value() default {};

		Component component() default @Component();
	}

	@Retention(RUNTIME)
	@Target(METHOD)
	public static @interface Filter {

		public String namespace();

		public String name() default "";

	}

	public static enum Mode {
		BUILD, RUN;
	}

}
