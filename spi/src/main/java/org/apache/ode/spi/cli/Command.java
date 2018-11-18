package org.apache.ode.spi.cli;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

@Qualifier
@Retention(RUNTIME)
@Target(METHOD)
public @interface Command {
	String entity() default "";

	String name();

	Option[] options();

	public static @interface Option {

		String name();

		String alias() default "";
	}
}
