package org.apache.ode.spi.deployment;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Scope;

import org.apache.ignite.Ignite;
import org.apache.ignite.binary.BinaryObject;

//Marker interface
public interface Assembler {

	@Target({ TYPE, METHOD })
	@Retention(RUNTIME)
	public @interface AssemblyType {
		String value();
	}

	@Target({ METHOD })
	@Retention(RUNTIME)
	public @interface Create {

	}

	@Target({ METHOD })
	@Retention(RUNTIME)
	public @interface Export {

	}

	@Target({ METHOD })
	@Retention(RUNTIME)
	public @interface Update {
		UpdateType type() default UpdateType.CONFIG;
	}

	public static enum UpdateType {
		CONFIG, FILE;
	}

	@Target({ METHOD })
	@Retention(RUNTIME)
	public @interface Delete {

	}

	@Target({ METHOD })
	@Retention(RUNTIME)
	public @interface Deploy {

	}

	@Target({ METHOD })
	@Retention(RUNTIME)
	public @interface Undeploy {

	}

	@Target({ METHOD })
	@Retention(RUNTIME)
	public @interface Stage {
		String value();
	}

	@Target({ TYPE, METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	public @interface Priority {
		int value();
	}

	public static BinaryObject assemblyTypeConfigPath(Ignite ignite, String assemblyType) {
		return ignite.binary().builder("ConfigurationKey").setField("path", "/ode:assemblies/" + assemblyType).build();
	}

	public static BinaryObject contentTypeConfigPath(Ignite ignite, String assemblyType) {
		return ignite.binary().builder("ConfigurationKey").setField("path", "/ode:contentType/" + assemblyType).build();
	}

	public static enum AssemblyStatus {
		DEPLOYED, UNDEPLOYED;
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

	// @NormalScope(passivating = false)
	@Scope
	@Inherited
	@Retention(RUNTIME)
	@Target({ TYPE, METHOD, FIELD })
	public @interface AssembleScoped {
	}

}
