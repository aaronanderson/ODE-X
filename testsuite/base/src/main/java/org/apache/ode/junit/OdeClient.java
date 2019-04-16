package org.apache.ode.junit;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

@Target({ TYPE })
@Retention(RUNTIME)
@ExtendWith(ODEClientExtension.class)
public @interface OdeClient {

	String config() default "ode-test.yml";

}
