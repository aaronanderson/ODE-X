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
package org.apache.ode.wsht.plugin.cdi;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.inject.Singleton;

import org.apache.ode.spi.cdi.Handler;
import org.apache.ode.wsht.WSHT;
import org.apache.ode.wsht.compiler.WSHTContextImpl;
import org.apache.ode.wsht.exec.WSHTComponent;
import org.apache.ode.wsht.spi.WSHTContext;

public class WSHTHandler extends Handler {

	@Singleton
	public static class BPELCompilerProducer {

		@Produces
		@Dependent
		public WSHTContext createWSHTCtx() {
			return new WSHTContextImpl();
		}

	}

	@Override
	public void beforeBeanDiscovery(BeforeBeanDiscovery bbd, BeanManager bm) {
		bbd.addAnnotatedType(bm.createAnnotatedType(WSHT.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(WSHTComponent.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(WSHTContextImpl.class));
	}

	public void afterDeploymentValidation(AfterDeploymentValidation adv, BeanManager bm) {
		manage(WSHT.class);
		start(bm);

	}

	@Override
	public void beforeShutdown(BeforeShutdown adv, BeanManager bm) {
		stop();
	}

}
