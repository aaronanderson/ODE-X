package org.apache.ode.arch.gme;

import org.junit.rules.ExternalResource;

import com.google.inject.Guice;
import com.google.inject.Module;

public class GuiceExternalResource extends ExternalResource {

	protected TestGuiceDIContainer container;
	protected Module[] modules;

	public GuiceExternalResource(Module... modules) {
		this.modules = modules;
	}

	@Override
	protected void before() throws Throwable {
		container = new TestGuiceDIContainer(Guice.createInjector(modules));
	};

	@Override
	protected void after() {
		container.destroy();
	};

}
