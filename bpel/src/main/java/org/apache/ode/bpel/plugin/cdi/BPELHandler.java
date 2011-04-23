/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ode.bpel.plugin.cdi;

import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.util.AnnotationLiteral;

import org.apache.ode.bpel.plugin.BPELPlugin;
import org.apache.ode.bpel.repo.BPELValidation;
import org.apache.ode.spi.cdi.Handler;

public class BPELHandler extends Handler {
	
	Bean<BPELPlugin> pluginBean;
	CreationalContext<BPELPlugin> pluginCtx;
	BPELPlugin pluginSys;

	@Override
	public void beforeBeanDiscovery(BeforeBeanDiscovery bbd, BeanManager bm) {
		bbd.addAnnotatedType(bm.createAnnotatedType(BPELPlugin.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(BPELValidation.class));
	}
	
	public void afterDeploymentValidation(AfterDeploymentValidation adv, BeanManager bm) {
		Set<Bean<?>> beans = bm.getBeans(BPELPlugin.class, new AnnotationLiteral<Any>() {
		});
		if (beans.size() > 0) {
			pluginBean = (Bean<BPELPlugin>)beans.iterator().next();
			pluginCtx = bm.createCreationalContext(pluginBean);
			bm.getReference(pluginBean, BPELPlugin.class, pluginCtx);
		} else {
			System.out.println("Can't find class " + BPELPlugin.class);
		}

	}
	
	@Override
	public void beforeShutdown(BeforeShutdown adv, BeanManager bm) {
	  if (pluginSys!=null){
		  pluginBean.destroy(pluginSys, pluginCtx);
	  }
	}


}
