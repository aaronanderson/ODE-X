package org.apache.ode.arch.gme;

import org.apache.ode.di.guice.core.GuiceDIContainer;
import org.apache.ode.test.core.TestDIContainer;
import org.apache.onami.lifecycle.standard.Disposer;

import com.google.inject.Injector;

public class TestGuiceDIContainer  extends GuiceDIContainer implements TestDIContainer {

	public TestGuiceDIContainer(Injector injector) {
		this.injector = injector;
		//System.out.println("Create thread" + Thread.currentThread());
		TestDIContainer.CONTAINER.set(this);
	}

	public void destroy() {
		//System.out.println("Destroy thread" + Thread.currentThread());
		TestDIContainer.CONTAINER.set(null);
		injector.getInstance(Disposer.class).dispose();
	}

	
}
