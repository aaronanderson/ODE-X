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
package org.apache.ode.server.cdi;

import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.util.AnnotationLiteral;
import org.apache.ode.runtime.build.BuildSystem;
import org.apache.ode.spi.cdi.Handler;

public class RuntimeHandler extends Handler {
	
	Bean<BuildSystem> buildSysBean;
	CreationalContext<BuildSystem> buildSysCtx;
	BuildSystem buildSys;
	
	public void beforeBeanDiscovery(BeforeBeanDiscovery bbd, BeanManager bm) {
		bbd.addAnnotatedType(bm.createAnnotatedType(BuildSystem.class));

	}

	public void afterDeploymentValidation(AfterDeploymentValidation adv, BeanManager bm) {
		Set<Bean<?>> beans = bm.getBeans(BuildSystem.class, new AnnotationLiteral<Any>() {
		});
		if (beans.size() > 0) {
			buildSysBean = (Bean<BuildSystem>)beans.iterator().next();
			buildSysCtx = bm.createCreationalContext(buildSysBean);
			bm.getReference(buildSysBean, BuildSystem.class, buildSysCtx);
		} else {
			System.out.println("Can't find class " + BuildSystem.class);
		}

	}
	
	@Override
	public void beforeShutdown(BeforeShutdown adv, BeanManager bm) {
	  if (buildSys!=null){
		  buildSysBean.destroy(buildSys, buildSysCtx);
	  }
	}
	
	
}
