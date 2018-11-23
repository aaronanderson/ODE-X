package org.apache.ode.spi.config;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

import org.apache.ignite.configuration.IgniteConfiguration;

/*
 Configure ignite via plugable service loaders so that the initiated Ignite can be injected. Also allows CLI to be configured without CDI 
 */
public interface IgniteConfigurator {

	public void configure(Config config, IgniteConfiguration igniteConfiguration);

}
