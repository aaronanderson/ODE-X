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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.ode.server.xml.ServerType;
import org.apache.ode.spi.cdi.Handler;

public class ExecutorHandler extends Handler {


	Bean<ExecutorProducer> execBean;
	CreationalContext<ExecutorProducer> execCtx;
	ExecutorProducer exec;
	
	
	@Singleton
	public static class ExecutorProducer {
		
		@Inject
		ServerType serverConfig;
		
		ExecutorService service;
		
		
		@Produces
		public ExecutorService create() {
			return service;
		}

	
		@PostConstruct
		public void init() {
			System.out.println("Starting Executor ");
			service = new ThreadPoolExecutor(0, 10, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(100));
		}

		@PreDestroy
		public void destroy() {
			System.out.println("Stopping Executor ");
			try{
			  service.shutdown();
			  service.awaitTermination(30, TimeUnit.SECONDS);
		  }catch (Exception e){
			  e.printStackTrace();
		  }
		}

	}


	public void beforeBeanDiscovery(BeforeBeanDiscovery bbd, BeanManager bm){
		bbd.addAnnotatedType(bm.createAnnotatedType(ExecutorProducer.class));
	}

	public void afterDeploymentValidation(AfterDeploymentValidation adv, BeanManager bm) {
		Set<Bean<?>> beans = bm.getBeans(ExecutorProducer.class, new AnnotationLiteral<Any>() {
		});
		if (beans.size() > 0) {
			execBean = (Bean<ExecutorProducer>) beans.iterator().next();
			execCtx = bm.createCreationalContext(execBean);
			exec = (ExecutorProducer) bm.getReference(execBean, ExecutorProducer.class, execCtx);
		} else {
			System.out.println("Can't find class " + ExecutorProducer.class);
		}

	}

	@Override
	public void beforeShutdown(BeforeShutdown adv, BeanManager bm) {
		if (exec != null) {
			execBean.destroy(exec, execCtx);
		}
	}

}
