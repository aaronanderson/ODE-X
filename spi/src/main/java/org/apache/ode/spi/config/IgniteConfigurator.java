package org.apache.ode.spi.config;

import java.util.Optional;

import org.apache.ignite.configuration.IgniteConfiguration;

/*
 Configure ignite via plugable service loaders so that the initiated Ignite can be injected. Also allows CLI to be configured without CDI 
 */
public interface IgniteConfigurator {

	public void configure(Config config, IgniteConfiguration igniteConfiguration);

}
