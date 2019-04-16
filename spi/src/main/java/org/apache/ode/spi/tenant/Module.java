package org.apache.ode.spi.tenant;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;

//Marker interface
public interface Module {

	@Inherited
	@Qualifier
	@Target({ TYPE, FIELD })
	@Retention(RUNTIME)
	public @interface Id {
		String value();

		@Nonbinding
		String[] dependencies() default {};
	}

	@Target({ METHOD })
	@Retention(RUNTIME)
	public @interface Enable {

	}

	@Target({ METHOD })
	@Retention(RUNTIME)
	public @interface Disable {

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
