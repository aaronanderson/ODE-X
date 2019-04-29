package org.apache.ode.spi.deployment;

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
public interface Composite {

	@Inherited
	@Qualifier
	@Target({ TYPE, FIELD })
	@Retention(RUNTIME)
	public @interface Id {
		String value();
	}

	@Target({ METHOD })
	@Retention(RUNTIME)
	public @interface Create {

	}

	@Target({ METHOD })
	@Retention(RUNTIME)
	public @interface Update {

	}

	@Target({ METHOD })
	@Retention(RUNTIME)
	public @interface Delete {

	}

	@Target({ METHOD })
	@Retention(RUNTIME)
	public @interface Activate {

	}

	@Target({ METHOD })
	@Retention(RUNTIME)
	public @interface Deactivate {

	}

	public static enum CompositeStatus {
		ACTIVE, INACTIVE;
	}

	public static class AssemblyException extends Exception {

		public AssemblyException(String msg) {
			this(msg, null);
		}

		public AssemblyException(Throwable t) {
			this(null, t);
		}

		public AssemblyException(String msg, Throwable t) {
			super(msg, t);
		}
	}

}
