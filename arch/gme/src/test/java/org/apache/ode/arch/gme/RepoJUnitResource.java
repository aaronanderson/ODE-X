package org.apache.ode.arch.gme;

import org.apache.ode.di.guice.core.JSR250Module;
import org.apache.ode.di.guice.memory.MemoryRepoModule;
import org.apache.ode.test.data.repo.RepoTest;
import org.junit.ClassRule;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;

@RunWith(Suite.class)
@SuiteClasses({ RepoTest.class })
public class RepoJUnitResource {
	public static GuiceDIContainer container;

	@ClassRule
	public static ExternalResource resource = new ExternalResource() {
		@Override
		protected void before() throws Throwable {
			container = new GuiceDIContainer(Guice.createInjector(new MemoryRepoModule()));
		};

		@Override
		protected void after() {
			container.destroy();
		};
	};

	public static class RepoModule extends AbstractModule {

		protected void configure() {
			install(new JSR250Module());
			//install(new DIModule(new OperationAnnotationProcessor()));

		}

	}

}
