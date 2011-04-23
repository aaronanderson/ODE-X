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

import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.util.AnnotationLiteral;
import javax.management.ObjectName;

import org.apache.ode.api.Repository;
import org.apache.ode.repo.ArtifactDataSourceImpl;
import org.apache.ode.repo.RepoCommandMap;
import org.apache.ode.repo.RepoFileTypeMap;
import org.apache.ode.repo.RepositoryImpl;
import org.apache.ode.repo.RepositoryMBean;
import org.apache.ode.server.JMXServer;
import org.apache.ode.spi.cdi.Handler;

public class RepoHandler extends Handler {
	
	public static class Dependent extends AnnotationLiteral<javax.enterprise.context.Dependent> implements javax.enterprise.context.Dependent {
	};
	
	public void beforeBeanDiscovery(BeforeBeanDiscovery bbd, BeanManager bm) {
		bbd.addAnnotatedType(bm.createAnnotatedType(RepoFileTypeMap.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(RepoCommandMap.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(ArtifactDataSourceImpl.class));
		//RepositoryImpl should have the dependent CDI scope so a new instance is bound
		//to the object it's being injected into's lifecycle
		AnnotatedTypeImpl<?> at = new AnnotatedTypeImpl(bm.createAnnotatedType(RepositoryImpl.class));
		at.getAnnotations().add(new Dependent());
		bbd.addAnnotatedType(at);

	}

	public void afterDeploymentValidation(AfterDeploymentValidation adv, BeanManager bm) {
		Set<Bean<?>> beans = bm.getBeans(JMXServer.class, new AnnotationLiteral<Any>() {
		});
		if (beans.size() > 0) {
			Bean<?> bean = beans.iterator().next();
			JMXServer server = (JMXServer) bm.getReference(bean, JMXServer.class, bm.createCreationalContext(bean));
			try {
				server.getMBeanServer().registerMBean(new RepositoryMBean(), ObjectName.getInstance(Repository.OBJECTNAME));
			} catch (Exception e) {
				e.printStackTrace();
				adv.addDeploymentProblem(e);
			} 
		}
	}
	
	
}
