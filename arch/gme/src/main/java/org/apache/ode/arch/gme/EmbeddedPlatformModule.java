package org.apache.ode.arch.gme;

import org.apache.ode.di.guice.core.JSR250Module;
import org.apache.ode.di.guice.jcache.JCacheModule;
import org.apache.ode.di.guice.memory.MemoryRepoModule;
import org.apache.ode.di.guice.runtime.DIDiscoveryModule;

import com.google.inject.AbstractModule;

public class EmbeddedPlatformModule extends AbstractModule {
	
	
	protected void configure() {
		//Platform
		install(new JSR250Module());
		install(new JCacheModule());
		//scanner
		install(new DIDiscoveryModule());
		
		//Data
		//Repository
		install(new MemoryRepoModule());
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