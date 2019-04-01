package org.apache.ode.runtime.tenant;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.ignite.Ignite;
import org.apache.ode.spi.tenant.Module;

@ApplicationScoped
public class CoreModuleImpl implements Module {

	public static final String CORE_MODULE_ID = "org:apache:ode:core";
	@Inject
	Ignite ignite;

	@Override
	public String id() {
		return CORE_MODULE_ID;
	}

	@Override
	public void enable() {

	}

	@Override
	public void disable() {

	}

}
