package org.apache.ode.arch.gme.runtime;

import org.apache.ode.arch.gme.TestGuiceDIContainer;
import org.apache.ode.arch.gme.GuiceExternalResource;
import org.apache.ode.di.guice.core.JSR250Module;
import org.apache.ode.di.guice.runtime.DIDiscoveryModule;
import org.apache.ode.test.core.scanner.ComponentTest;
import org.apache.ode.test.core.scanner.ComponentTest.ComponentRegistry;
import org.apache.ode.test.core.scanner.ComponentTest.TestComponent;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.google.inject.AbstractModule;

@RunWith(Suite.class)
@SuiteClasses({ ComponentTest.class })
public class DIAnnotationTest {
	public static TestGuiceDIContainer container;

	@ClassRule
	public static GuiceExternalResource resource = new GuiceExternalResource((new TestDIModule()));

	public static class TestDIModule extends AbstractModule {

		protected void configure() {
			install(new JSR250Module());
			install(new DIDiscoveryModule());
			bind(TestComponent.class);
			bind(ComponentRegistry.class);

		}

	}

}
