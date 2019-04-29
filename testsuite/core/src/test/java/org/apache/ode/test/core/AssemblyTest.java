package org.apache.ode.test.core;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.ode.junit.OdeServer;
import org.apache.ode.runtime.Server;
import org.apache.ode.spi.deployment.Assembly;
import org.apache.ode.spi.deployment.Assembly.Id;
import org.apache.ode.spi.deployment.AssemblyManager;
import org.junit.jupiter.api.Test;

@OdeServer
public class AssemblyTest {
	Server server = null;

	public AssemblyTest(Server server) {
		this.server = server;
	}

	//@Test
	public void lifecycle() throws Exception {
		AssemblyManager assemblyManager = server.ignite().services().serviceProxy(AssemblyManager.SERVICE_NAME, AssemblyManager.class, false);
		assertNotNull(assemblyManager);
	}

	@Id("urn:org:apache:ode:assembly:test")
	public static class TestAssembly implements Assembly {

	}

}
	