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

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.enterprise.inject.spi.ProcessObserverMethod;
import javax.enterprise.inject.spi.ProcessProducer;

import org.apache.ode.spi.cdi.Handler;

//import org.junit.Ignore;

//@Ignore
public class StaticHandler extends Handler {

	private static List<Handler> delegates = new ArrayList<Handler>();

	public static void addDelegate(Handler handler) {
		delegates.add(handler);
	}
	
	public static void clear() {
		delegates.clear();
	}

	public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd, BeanManager bm){
		for (Handler h: delegates){
			h.beforeBeanDiscovery(bbd, bm);
		}
	}
	
	public void afterBeanDiscovery(@Observes AfterBeanDiscovery abd, BeanManager bm){
		for (Handler h: delegates){
			h.afterBeanDiscovery(abd, bm);
		}
	}
	
	public void afterDeploymentValidation(@Observes AfterDeploymentValidation adv, BeanManager bm){
		for (Handler h: delegates){
			h.afterDeploymentValidation(adv, bm);
		}
	}
	
	public void beforeShutdown(@Observes BeforeShutdown bfs, BeanManager bm){
		//Reverse the order
		for (int i= delegates.size()-1; i>-1;i--){
			delegates.get(i).beforeShutdown(bfs, bm);
		}
	}
	
	public void processAnnotatedType(@Observes ProcessAnnotatedType<?> pa, BeanManager bm){
		for (Handler h: delegates){
			h.processAnnotatedType(pa, bm);
		}
	}
	
	public void processInjectionTarget(@Observes ProcessInjectionTarget<?> pit, BeanManager bm){
		for (Handler h: delegates){
			h.processInjectionTarget(pit, bm);
		}
	}
	
	public void processProducer(@Observes ProcessProducer<?,?> ppd, BeanManager bm){
		for (Handler h: delegates){
			h.processProducer(ppd, bm);
		}
	}
	
	public void processBean(@Observes ProcessBean<?> pb, BeanManager bm){
		for (Handler h: delegates){
			h.processBean(pb, bm);
		}
	}
	
	public void processObserverMethod(@Observes ProcessObserverMethod<?,?> pom, BeanManager bm){
		for (Handler h: delegates){
			h.processObserverMethod(pom, bm);
		}
	}

}
