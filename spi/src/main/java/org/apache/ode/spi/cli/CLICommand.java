package org.apache.ode.spi.cli;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;

@Qualifier
@Retention(RUNTIME)
@Target(TYPE)
public @interface CLICommand {

	String entity();

	String name();

	@Nonbinding
	Option[] options();

	public static @interface Option {

		String name();

		String alias() default "";

		Type type() default Type.STRING;

	}

	public static enum Type {

		STRING, INTEGER, FILE;
	}
}
