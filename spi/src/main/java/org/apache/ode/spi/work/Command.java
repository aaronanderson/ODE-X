package org.apache.ode.spi.work;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.apache.ode.spi.runtime.Component;

//Commands are needed declare the contract for a command and so that input/output parameters can be validated for order and type
@Retention(RUNTIME)
@Target(METHOD)
public @interface Command {

	String namespace() default "";

	String name() default "";
	
	Component component() default @Component();

	@Retention(RUNTIME)
	@Target(TYPE)
	public @interface CommandSet {
		public String namespace() default "";
		
		Component component() default @Component();

	}

	//This is needed because in DI containers like guice if an interface is registered with the DI container and there is no implementation bound as well an error is generated
	@Retention(RUNTIME)
	@Target(TYPE)
	public @interface CommandSetRef {
		public Class<?>[] value() default {};

	}

}
