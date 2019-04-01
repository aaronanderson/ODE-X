package org.apache.ode.spi.tenant;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.ode.spi.tenant.Module.ModuleStatus;

public interface Tenant {
	public static final String SERVICE_NAME = "urn:org:apache:ode:tenant";
	public static final String ALL_MODULES = "urn:org:apache:ode:tenant#all_modules";

	Set<String> modules();

	<M> M instance(String moduleId) throws ModuleException;

	ModuleStatus status(String moduleId) throws ModuleException;

	void enable(String moduleId) throws ModuleException;

	void disable(String moduleId) throws ModuleException;

	TenantStatus status();

	void awaitStart(long timeout, TimeUnit unit);

	public static enum TenantStatus {
		ONLINE, OFFLINE;
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
