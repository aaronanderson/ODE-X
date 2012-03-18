package org.apache.ode.server.cdi;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;

import org.apache.ode.runtime.exec.platform.ExecutableScopeContext;
import org.apache.ode.spi.exec.ExecutableScope;

/*
 * I really hate thread locals but it seems that is the only way to get non-trivial custom scopes to work.
 */
@Dependent
public class ExecutableScopeContextImpl implements ExecutableScopeContext {

	ThreadScope scope = null;
	static ThreadLocal<ThreadScope> threadLocal = new ThreadLocal<ThreadScope>();

	static class ThreadScope {
		Map<Class<?>, Map<String, ScopedInstance<?>>> scopes;
		Set<ScopedInstance> instances;

	}

	@Inject
	BeanManager bm;

	@Override
	public void create() {
		scope = new ThreadScope();
		scope.scopes = new HashMap<Class<?>, Map<String, ScopedInstance<?>>>();
		scope.instances = new HashSet<>();
	}

	@Override
	public void begin() {
		threadLocal.set(scope);
	}

	@Override
	public void end() {
		threadLocal.remove();
	}

	@Override
	public <T> T newInstance(Class<T> clazz) {
		Set<Bean<?>> beans = bm.getBeans(clazz, new AnnotationLiteral<Any>() {
		});
		if (beans.size() > 0) {
			Bean bean = beans.iterator().next();
			CreationalContext cc = bm.createCreationalContext(bean);
			return (T) bm.getReference(bean, clazz, cc);
		}
		return null;
	}

	@Override
	public void wrap(Object unmanaged) {

	}

	@Override
	public void destroy() {
		for (ScopedInstance entry2 : scope.instances) {
			entry2.bean.destroy(entry2.instance, entry2.ctx);
		}
		scope = null;
	}

	public static class ScopedInstance<T> {
		Bean<T> bean;
		CreationalContext<T> ctx;
		T instance;
	}

	public static class CDIContextImpl implements Context {

		@Override
		public Class<? extends Annotation> getScope() {
			return ExecutableScope.class;
		}

		@Override
		public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
			ScopedInstance si = null;
			Bean bean = (Bean) contextual;
			ThreadScope tscope = threadLocal.get();

			if (bean.getName() != null) {
				Map<Class<?>, Map<String, ScopedInstance<?>>> scopes = tscope.scopes;
				Map<String, ScopedInstance<?>> scoped = scopes.get(bean.getBeanClass());
				if (scoped == null) {
					scoped = new HashMap<String, ScopedInstance<?>>();
					scopes.put(bean.getBeanClass(), scoped);
				}
				si = scoped.get(bean.getName());
				if (si == null) {
					si = new ScopedInstance();
					si.bean = bean;
					si.ctx = creationalContext;
					si.instance = bean.create(creationalContext);
					tscope.instances.add(si);
					scoped.put(bean.getName(), si);
				}
			} else {
				si = new ScopedInstance();
				si.bean = bean;
				si.ctx = creationalContext;
				tscope.instances.add(si);
				si.instance = bean.create(creationalContext);
			}
			System.out.println("returning1 " + si.instance + " cc " + creationalContext + " bean " + bean);
			return (T) si.instance;
		}

		@Override
		public <T> T get(Contextual<T> contextual) {
			throw new IllegalArgumentException();
			/*
			Bean bean = (Bean) contextual;
			System.out.println("returning2 " + bean.getName() + " bean " + bean);

			return null;*/
		}

		@Override
		public boolean isActive() {
			return threadLocal.get() != null ? true : false;
		}

	}

	/* Used for wrapping object
	class ExecutableScopeBean implements Bean<?> {
		
		Object delegate;

		public Set<Type> getTypes() {
			Set<Type> types = new HashSet<Type>();
			types.add(ServerConfig.class);
			types.add(Object.class);
			return types;
		}

		public Set<Annotation> getQualifiers() {
			Set<Annotation> qualifiers = new HashSet<Annotation>();
			qualifiers.add(new AnnotationLiteral<Default>() {

			});
			qualifiers.add(new AnnotationLiteral<Any>() {

			});
			return qualifiers;

		}

		public Class<? extends Annotation> getScope() {
			return ExecutableScope.class;
		}

		public String getName() {
			return delegate.class.getSimpleName();
		}

		public Set<Class<? extends Annotation>> getStereotypes() {
			return Collections.EMPTY_SET;
		}

		public Class<?> getBeanClass() {
			return delegate.getClass();
		}

		public boolean isAlternative() {
			return false;
		}

		public boolean isNullable() {
			return false;
		}

		public Set<InjectionPoint> getInjectionPoints() {
			return it.getInjectionPoints();
		}

		@Override
		public Object create(CreationalContext<?> ctx) {
			return delegate;

		}

		@Override
		public void destroy(Object instance, CreationalContext<?> ctx) {
		}
	};
	*/

}
