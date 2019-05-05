package org.apache.ode.dev;

import org.apache.ignite.Ignite;
import org.apache.ode.spi.deployment.DevelopmentManager;
import org.apache.ode.spi.tenant.Module;
import org.apache.ode.spi.tenant.Module.Id;

@Id(DevelopmentModule.DEVELOPMENT_MODULE_ID)
public class DevelopmentModule implements Module {

	public static final String DEVELOPMENT_MODULE_ID = "org:apache:ode:development";

	@Enable
	public void enable(Ignite ignite) {
		ignite.services(ignite.cluster().forLocal()).deployNodeSingleton(DevelopmentManager.SERVICE_NAME, new DevelopmentManagerImpl());
	}

	@Disable
	public void disable(Ignite ignite) {
		ignite.services(ignite.cluster().forLocal()).cancel(DevelopmentManager.SERVICE_NAME);
	}

}
