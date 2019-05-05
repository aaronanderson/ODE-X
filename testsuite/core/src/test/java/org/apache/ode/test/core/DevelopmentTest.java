package org.apache.ode.test.core;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.ode.junit.OdeServer;
import org.apache.ode.runtime.Server;
import org.apache.ode.spi.deployment.Assembly;
import org.apache.ode.spi.deployment.Assembly.Id;
import org.apache.ode.spi.deployment.AssemblyManager;
import org.apache.ode.spi.deployment.DevelopmentManager;
import org.junit.jupiter.api.Test;

@OdeServer
public class DevelopmentTest {
	Server server = null;

	public DevelopmentTest(Server server) {
		this.server = server;
	}

	@Test
	public void lifecycle() throws Exception {
		DevelopmentManager developmentManager = server.ignite().services().service(DevelopmentManager.SERVICE_NAME);
		assertNotNull(developmentManager);
	}

	@Id("urn:org:apache:ode:assembly:test")
	public static class TestAssembly implements Assembly {

	}

}
