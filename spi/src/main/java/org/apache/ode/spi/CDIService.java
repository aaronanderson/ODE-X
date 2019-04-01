package org.apache.ode.spi;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.InjectionTargetFactory;

import org.apache.ignite.services.Service;
import org.apache.ignite.services.ServiceContext;

public class CDIService implements Service {

	private InjectionTarget injectionTarget;
	private CreationalContext creationalContext;

	@Override
	public void init(ServiceContext ctx) throws Exception {
		BeanManager bm = CDI.current().getBeanManager();
		InjectionTargetFactory itf = bm.getInjectionTargetFactory(bm.createAnnotatedType(getClass()));
		injectionTarget = itf.createInjectionTarget(null);
		creationalContext = bm.createCreationalContext(null);
		injectionTarget.inject(this, creationalContext);
		injectionTarget.postConstruct(this);
	}

	@Override
	public void cancel(ServiceContext ctx) {
		injectionTarget.preDestroy(this);
		creationalContext.release();
	}

	@Override
	public void execute(ServiceContext ctx) throws Exception {

	}

}
