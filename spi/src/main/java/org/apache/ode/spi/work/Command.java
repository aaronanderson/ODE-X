package org.apache.ode.spi.work;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(TYPE)
public @interface Command {

	String namespace() default "";

	String name() default "";

	@Retention(RUNTIME)
	@Target(TYPE)
	public @interface CommandSet {
		public String namespace() default "";

	}

}