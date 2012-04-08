package org.apache.ode.spi.exec;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Scope;

/**
 *	Any object in this scope is bound to the JAXB executable loaded for execution. If the annotated class has the named annotation then it can
 *be shared in the scope, otherwise no instances are shared. This scope is only useful for runtime instructions. It is guaranteed that
 *the postCreate and preDestroy methods are run during the lifetime of the loaded XML executable
 */
@Scope
@Retention(RUNTIME)
@Target(TYPE)
public @interface ThreadScope {
	boolean singleton() default false;
}
