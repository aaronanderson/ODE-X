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
package org.apache.ode.spi.cdi;

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

public class Handler {
	public void beforeBeanDiscovery(BeforeBeanDiscovery bbd, BeanManager bm){}
		
	public void afterBeanDiscovery(AfterBeanDiscovery adv, BeanManager bm){}
	
	public void afterDeploymentValidation(AfterDeploymentValidation adv, BeanManager bm){}
	
	public void beforeShutdown(BeforeShutdown adv, BeanManager bm){}
	
	public void processAnnotatedType(ProcessAnnotatedType<?> adv, BeanManager bm){}
	
	public void processInjectionTarget(ProcessInjectionTarget<?> adv, BeanManager bm){}
	
	public void processProducer(ProcessProducer<?,?> adv, BeanManager bm){}
	
	public void processBean(ProcessBean<?> adv, BeanManager bm){}
	
	public void processObserverMethod(ProcessObserverMethod<?,?> adv, BeanManager bm){}

}
