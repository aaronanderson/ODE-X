package org.apache.ode.spi.exec;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Scope;

/**
 *	Any object in this scope is bound to the ODE process context. If the annotated class has the named annotation then it can
 *be shared in the scope, otherwise no instances are shared. This scope is useful for sharing data across instruction and thread invocations. Objects
 *stored in this scope should be thread safe as the runtime will concurrently execute multiple Java threads with the same scope instance.
 */
@Scope
@Retention(RUNTIME)
@Target(TYPE)
public @interface ProcessScope {

}
