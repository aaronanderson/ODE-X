package org.apache.ode.spi.tenant;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.ode.spi.tenant.Module.ModuleException;
import org.apache.ode.spi.tenant.Module.ModuleStatus;

public interface Tenant {
	public static final String SERVICE_NAME = "urn:org:apache:ode:tenant";
	public static final String ALL_MODULES = "urn:org:apache:ode:tenant#all_modules";
	public static final String STATUS_TOPIC = "urn:org:apache:ode:tenant#status_topic";
	
	public static final String TENANT_CACHE_NAME = "Tenant";
	public static final String PROCESS_CACHE_NAME = "Process";


	String name();
	
	Set<String> modules();
	
	ModuleStatus status(String moduleId) throws ModuleException;

	void enable(String moduleId) throws ModuleException;

	void disable(String moduleId) throws ModuleException;

	TenantStatus status();

	void status(TenantStatus status);

	void awaitInitialization(long timeout, TimeUnit unit) throws InterruptedException;

	public static enum TenantStatus {
		ONLINE, OFFLINE;
	}

}
