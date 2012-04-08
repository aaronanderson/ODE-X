package org.apache.ode.spi.exec;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Scope;

/**
 *	Any object in this scope is bound to a single ODE instruction invocation. No instances are shared. This scope is useful for obtaining one time use instruction operation. Objects
 *stored in this scope are considered to be thread safe and the runtime will not concurrently execute multiple Java threads with the same scope instance.
 */
@Scope
@Retention(RUNTIME)
@Target(TYPE)
public @interface InstructionScope {
	boolean singleton() default false;
}
