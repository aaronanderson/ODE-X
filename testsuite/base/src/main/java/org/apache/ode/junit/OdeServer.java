package org.apache.ode.junit;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

@Target({ TYPE })
@Retention(RUNTIME)
@ExtendWith(ODEServerExtension.class)
public @interface OdeServer {

	String config() default "ode-test.yml";

}
