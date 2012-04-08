/**
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

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Singleton;

import org.apache.ode.bpel.BPEL;
import org.apache.ode.bpel.compiler.BPELContextImpl;
import org.apache.ode.bpel.exec.BPELComponent;
import org.apache.ode.bpel.spi.BPELContext;
import org.apache.ode.spi.cdi.Handler;

public class BPELHandler extends Handler {

	@Singleton
	public static class BPELCompilerProducer {

		@Produces
		@Dependent
		public BPELContext createBPELCtx() {
			return new BPELContextImpl();
		}

	}

	@Override
	public void beforeBeanDiscovery(BeforeBeanDiscovery bbd, BeanManager bm) {
		bbd.addAnnotatedType(bm.createAnnotatedType(BPEL.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(BPELComponent.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(BPELContextImpl.class));
	}

	public void afterDeploymentValidation(AfterDeploymentValidation adv, BeanManager bm) {
		manage(BPEL.class);
		start(bm);

	}

	@Override
	public void beforeShutdown(BeforeShutdown adv, BeanManager bm) {
		stop();
	}

}
