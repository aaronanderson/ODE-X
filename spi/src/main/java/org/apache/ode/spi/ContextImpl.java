package org.apache.ode.spi;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.spi.AlterableContext;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

//scopes should be isolated to a single thread execution so scope exists entirely in memory with no locking required
public abstract class ContextImpl implements AlterableContext {

	protected BeanManager manager;

	protected abstract ThreadLocal<ThreadLocalState> state();

	public void setBeanManager(BeanManager manager) {
		this.manager = manager;
	}

	@Override
	public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
		ThreadLocalState tscope = state().get();
		ScopedInstance si = tscope.instances.get(contextual);
		if (si == null) {
			si = new ScopedInstance();
			si.contextual = contextual;
			si.context = creationalContext;
			si.instance = contextual.create(creationalContext);
			tscope.instances.put(contextual, si);
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

	@Override
	public void destroy(Contextual<?> contextual) {
		ThreadLocalState tscope = state().get();
		ScopedInstance si = tscope.instances.get(contextual);
		if (si != null) {
			si.contextual.destroy(si.instance, si.context);
			tscope.instances.remove(contextual);
		}

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
		for (ScopedInstance si : state().get().instances.values()) {
			si.contextual.destroy(si.instance, si.context);
		}

		state().remove();

	}

	public <T> T instance(Class<T> type, Annotation... annotations) {
		Set<Bean<?>> beans = manager.getBeans(type, annotations);
		if (!beans.isEmpty()) {
			Bean bean = beans.iterator().next();
			CreationalContext cc = manager.createCreationalContext(bean);
			// Context ctx = manager.getContext(getScope());
			// return (T) ctx.get(bean, cc);
			return (T) manager.getReference(bean, type, cc);
		}
		return null;
	}

	public static class ScopedInstance<T> {
		Contextual<T> contextual;
		CreationalContext<T> context;
		T instance;
	}

	public static class ThreadLocalState {
		Map<Contextual<?>, ScopedInstance<?>> instances = new HashMap<>();

	}

}
