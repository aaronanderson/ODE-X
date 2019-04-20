package org.apache.ode.tomcat;

import org.apache.ignite.Ignite;
import org.apache.ode.spi.tenant.Module;
import org.apache.ode.spi.tenant.Module.Id;
import org.apache.ode.spi.webserver.WebServer;

@Id(TomcatModule.TOMCAT_MODULE_ID)
public class TomcatModule implements Module {

	public static final String TOMCAT_MODULE_ID = "org:apache:ode:tomcat";

	@Enable
	public void enable(Ignite ignite) {
		ignite.services().deployNodeSingleton(WebServer.SERVICE_NAME, new WebServerImpl());
	}

	@Disable
	public void disable(Ignite ignite) {
		ignite.services().cancel(WebServer.SERVICE_NAME);
	}

}
