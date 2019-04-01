package org.apache.ode.spi.config;

import org.apache.ignite.configuration.IgniteConfiguration;

public class IgniteConfigureEvent {

	private final Config config;
	private final IgniteConfiguration igniteConfig;

	public IgniteConfigureEvent(Config config, IgniteConfiguration igniteConfig) {
		this.config = config;
		this.igniteConfig = igniteConfig;
	}

	public Config config() {
		return config;
	}

	public IgniteConfiguration igniteConfig() {
		return igniteConfig;
	}

}
