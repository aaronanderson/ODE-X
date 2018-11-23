package org.apache.ode.runtime.cli;

import org.apache.ignite.IgniteException;
import org.apache.ode.spi.cli.CLICommand;
import org.apache.ode.spi.cli.CommandJob;
import org.apache.ode.spi.cli.CommandRequest;
import org.apache.ode.spi.cli.CommandResponse;
import org.apache.ode.spi.cli.CommandResponse.Status;

@CLICommand(entity = "tenant", name = "install", options = {})
public class Tenant extends CommandJob {

	@Override
	public CommandResponse execute(CommandRequest request) throws IgniteException {
		System.out.format("Command: %s %s %s\n", request.entity(), request.name(), request.parameters().get(0).name());
		CommandResponse response = new CommandResponse().message("Success");
		return response;
	}

}
