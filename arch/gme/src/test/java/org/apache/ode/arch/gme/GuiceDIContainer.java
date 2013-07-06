package org.apache.ode.arch.gme;

import org.apache.ode.test.core.DIContainer;
import org.apache.onami.lifecycle.standard.Disposer;

import com.google.inject.Injector;

public class GuiceDIContainer implements DIContainer {
	Injector injector;

	public GuiceDIContainer(Injector injector) {
		this.injector = injector;
		System.out.println("Create thread" + Thread.currentThread());
		DIContainer.CONTAINER.set(this);
	}

	public void destroy() {
		System.out.println("Destroy thread" + Thread.currentThread());
		DIContainer.CONTAINER.set(null);
		injector.getInstance(Disposer.class).dispose();
	}

	@Override
	public <T> T getInstance(Class<T> clazz) {
		return injector.getInstance(clazz);
	}

}
