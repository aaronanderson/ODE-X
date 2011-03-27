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
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.apache.ode.api.Repository;
import org.apache.ode.repo.RepositoryAPIImpl;
import org.apache.ode.repo.RepositorySPIImpl;
import org.apache.ode.server.JMXServer;
import org.apache.ode.spi.repo.RepoCommandMap;
import org.apache.ode.spi.repo.RepoFileTypeMap;

public class RepoHandler extends Handler {
	public void beforeBeanDiscovery(BeforeBeanDiscovery bbd, BeanManager bm) {
		bbd.addAnnotatedType(bm.createAnnotatedType(RepoFileTypeMap.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(RepoCommandMap.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(RepositorySPIImpl.class));

	}

	public void afterDeploymentValidation(AfterDeploymentValidation adv, BeanManager bm) {
		Set<Bean<?>> beans = bm.getBeans(JMXServer.class, new AnnotationLiteral<Any>() {
		});
		if (beans.size() > 0) {
			Bean<?> bean = beans.iterator().next();
			JMXServer server = (JMXServer) bm.getReference(bean, JMXServer.class, bm.createCreationalContext(bean));
			try {
				server.getMBeanServer().registerMBean(new RepositoryAPIImpl(), ObjectName.getInstance(Repository.OBJECTNAME));
			} catch (Exception e) {
				e.printStackTrace();
				adv.addDeploymentProblem(e);
			} 
		}
	}

	protected void start(Class clazz, BeanManager bm) {
		Set<Bean<?>> beans = bm.getBeans(clazz, new AnnotationLiteral<Any>() {
		});
		if (beans.size() > 0) {
			Bean<?> bean = beans.iterator().next();
			bm.getReference(bean, clazz, bm.createCreationalContext(bean));
		} else {
			System.out.println("Can't find class " + clazz);
		}

	}

}
