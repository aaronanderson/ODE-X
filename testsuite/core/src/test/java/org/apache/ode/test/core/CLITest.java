package org.apache.ode.test.core;

import org.apache.ignite.Ignite;
import org.apache.ode.junit.ODEServerExtension;
import org.apache.ode.runtime.Server;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith({ ODEServerExtension.class })
public class CLITest {
	Server server = null;
	Ignite client = null;

	public CLITest(Server server) {
		this.server = server;
	}

	@Test
	public void install() throws Exception {
	}
}
