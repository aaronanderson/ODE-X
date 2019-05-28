package org.apache.ode.spi;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;

import org.apache.ode.spi.ContextImpl.ThreadLocalState;

public abstract class ContextImpl implements Context {

	protected abstract ThreadLocal<ThreadLocalState> state();

	@Override
	public <T> T get(Contextual<T> component, CreationalContext<T> creationalContext) {
		Bean bean = (Bean) component;
		ThreadLocalState tscope = state().get();
		ScopedInstance si = tscope.instances.get(bean.getBeanClass());
		if (si == null) {
			si = new ScopedInstance();
			si.bean = bean;
			si.ctx = creationalContext;
			si.instance = bean.create(creationalContext);
			tscope.instances.put(bean.getBeanClass(), si);
		}

		return (T) si.instance;
	}

	@Override
	public <T> T get(Contextual<T> component) {
		throw new IllegalArgumentException();
	}

	@Override
	public boolean isActive() {
		return state().get() != null ? true : false;
	}

	public void start() {
		if (state().get() != null) {
			throw new IllegalAccessError("Already in scope");
		}
		state().set(new ThreadLocalState());
	}

	public void end() {
		if (state().get() == null) {
			throw new IllegalAccessError("Not in scope");
		}

		// normal scope handled by container
		for (ScopedInstance entry2 : state().get().instances.values()) {
			entry2.bean.destroy(entry2.instance, entry2.ctx);
		}

		state().remove();

	}

	public static class ScopedInstance<T> {
		Class<T> clazz;
		Bean<T> bean;
		CreationalContext<T> ctx;
		T instance;
	}

	public static class ThreadLocalState {
		Map<Class<?>, ScopedInstance<?>> instances = new HashMap<>();

	}

}
