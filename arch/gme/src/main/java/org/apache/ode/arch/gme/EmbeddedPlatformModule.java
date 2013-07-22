package org.apache.ode.arch.gme;

import org.apache.ode.di.guice.core.JSR250Module;
import org.apache.ode.di.guice.memory.data.RepoModule;
import org.apache.ode.di.guice.runtime.DIDiscoveryModule;

import com.google.inject.AbstractModule;

public class EmbeddedPlatformModule extends AbstractModule {
	
	
	protected void configure() {
		//Platform
		install(new JSR250Module());
		install(new RepoModule());
		//scanner
		install(new DIDiscoveryModule());
		
		//Data
		//Repository
		
		//ExecutionContext
		//ExecutionConfig
		
		//Runtime
		//Executors
		//Operations
		//Node
		
		
		//Bootstrap
		
		//
	}

}