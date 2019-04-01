package org.apache.ode.spi;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.InjectionTargetFactory;

import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.services.Service;
import org.apache.ignite.services.ServiceContext;

public abstract class CDICallable<V> implements IgniteCallable<V> {

	@Override
	public V call() throws Exception {
		BeanManager bm = CDI.current().getBeanManager();
		InjectionTargetFactory itf = bm.getInjectionTargetFactory(bm.createAnnotatedType(getClass()));
		InjectionTarget injectionTarget = itf.createInjectionTarget(null);
		CreationalContext creationalContext = bm.createCreationalContext(null);
		injectionTarget.inject(this, creationalContext);
		injectionTarget.postConstruct(this);
		try {
			return callExt();
		} finally {
			injectionTarget.preDestroy(this);
			creationalContext.release();
		}
	}

	public abstract V callExt() throws Exception;
}
