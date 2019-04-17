package org.apache.ode.test.core;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.ode.junit.OdeClient;
import org.apache.ode.junit.OdeServer;
import org.apache.ode.runtime.Client;
import org.apache.ode.runtime.Server;
import org.apache.ode.runtime.cli.CLITask;
import org.apache.ode.spi.cli.CommandRequest;
import org.apache.ode.spi.cli.CommandRequest.StringParameter;
import org.apache.ode.spi.cli.CommandResponse;
import org.junit.jupiter.api.Test;

@OdeServer
@OdeClient
public class CLITest {
	Server server = null;
	Client client = null;

	public CLITest(Server server, Client client) {
		this.server = server;
		this.client = client;
	}

	@Test
	public void install() throws Exception {
		assertNotNull(server.ignite());
		assertNotNull(server.container());
		assertNotNull(server.container().getBeanManager());
		assertNotNull(client);
		CommandResponse response = client.send(CLITask.CLI_TASK_NAME, new CommandRequest().entity("tenant").name("install").parameters(new StringParameter().name("test").value("test")));
		System.out.format("got response %s %s\n", response.status(), response.message());

	}

}
