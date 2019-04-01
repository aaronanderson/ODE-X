package org.apache.ode.spi.tenant;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

public interface Module {

	String id();

	default void enable() {
	}

	default void disable() {
	}

	public static enum ModuleStatus {
		ENABLED, DISABLED;
	}

}
