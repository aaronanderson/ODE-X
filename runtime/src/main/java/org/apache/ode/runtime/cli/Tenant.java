package org.apache.ode.runtime.cli;

import org.apache.ode.spi.cli.CLI;
import org.apache.ode.spi.cli.Command;
import org.apache.ode.spi.cli.Command.Option;
import org.apache.ode.spi.cli.Entity;

@CLI
@Entity("tenant")
public class Tenant {

	// @Command(name = "install", options = {@Option(name="--")})
	@Command(name = "install", options = {})
	public void install() {

	}

}
