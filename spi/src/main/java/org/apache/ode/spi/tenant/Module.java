package org.apache.ode.spi.tenant;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

public interface Module {

	String id();

	default void enable() throws ModuleException {
	}

	default void disable() throws ModuleException {
	}

	default String[] dependencies() {
		return new String[0];
	}

	public static enum ModuleStatus {
		ENABLED, DISABLED;
	}

	public static class ModuleException extends Exception {

		public ModuleException(String msg) {
			this(msg, null);
		}

		public ModuleException(Throwable t) {
			this(null, t);
		}

		public ModuleException(String msg, Throwable t) {
			super(msg, t);
		}
	}

}
