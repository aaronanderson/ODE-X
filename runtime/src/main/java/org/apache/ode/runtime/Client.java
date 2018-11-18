package org.apache.ode.runtime;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.ode.spi.config.Config;

public class Client implements AutoCloseable {

	public static final Logger LOG = LogManager.getLogger(Client.class);

	private static final Client CLIENT = new Client();
	private Ignite ignite;

	private Client() {
	}

	public Client start(String configFile) {
		Config odeConfig = Configurator.loadConfigFile(configFile);

		IgniteConfiguration serverConfig = new IgniteConfiguration();
		serverConfig.setClientMode(true);
		Configurator.configureIgnite(odeConfig, serverConfig);

		ignite = Ignition.start(serverConfig);

		return this;
	}

	public static Client instance() {
		return CLIENT;
	}

	public Ignite ignite() {
		if (ignite == null) {
			throw new IllegalStateException("Ignite unavailable");
		}
		return ignite;
	}

	@Override
	public void close() throws Exception {
		if (ignite != null) {
			ignite.close();
		}
		ignite = null;
	}

}
