package org.apache.ode.di.guice.core;

import java.lang.annotation.Annotation;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.ode.spi.di.DIContainer;

import com.google.inject.Injector;
import com.google.inject.Key;

@Singleton
public class GuiceDIContainer implements DIContainer {

	@Inject
	protected Injector injector;

	@Override
	public <T> T getInstance(Class<T> clazz) {
		return injector.getInstance(clazz);
	}

	@Override
	public <T> T getInstance(Class<T> clazz, Annotation qualifier) {
		return injector.getInstance(Key.get(clazz, qualifier));
	}

	@Override
	public <T> T getInstance(TypeLiteral<T> type) {
		return (T) injector.getInstance(Key.get(type.getType()));
	}

	@Override
	public <T> T getInstance(TypeLiteral<T> type, Annotation qualifier) {
		return (T) injector.getInstance(Key.get(type.getType(), qualifier));
	}

}
