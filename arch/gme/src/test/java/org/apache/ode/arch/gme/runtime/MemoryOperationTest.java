package org.apache.ode.arch.gme.runtime;

import org.apache.ode.arch.gme.GuiceExternalResource;
import org.apache.ode.arch.gme.TestGuiceDIContainer;
import org.apache.ode.di.guice.core.DIContainerModule;
import org.apache.ode.di.guice.core.JSR250Module;
import org.apache.ode.di.guice.runtime.DIDiscoveryModule;
import org.apache.ode.test.runtime.operation.InstanceTest;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.google.inject.AbstractModule;

@RunWith(Suite.class)
@SuiteClasses({ InstanceTest.class })
public class MemoryOperationTest {
	public static TestGuiceDIContainer container;

	@ClassRule
	public static GuiceExternalResource resource = new GuiceExternalResource((new TestDIContainerModule()));

	public static class TestDIContainerModule extends AbstractModule {

		protected void configure() {
			install(new JSR250Module());
			install(new DIContainerModule());
			install(new DIDiscoveryModule());
			
		}

	}

}
