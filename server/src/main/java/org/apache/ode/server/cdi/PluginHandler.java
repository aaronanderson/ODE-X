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

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.Set;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.util.AnnotationLiteral;

import org.apache.ode.spi.Plugin;

public class PluginHandler extends Handler{
	public void beforeBeanDiscovery(BeforeBeanDiscovery bbd, BeanManager bm) {

		try {
			ClassLoader currentCL = Thread.currentThread()
					.getContextClassLoader();
			Enumeration<URL> configs = currentCL
					.getResources("META-INF/services/org.apache.ode.spi.Plugin");
			while (configs.hasMoreElements()) {
				Scanner s = new Scanner(configs.nextElement().openStream());
				while (s.hasNext()) {
					Class clazz = Class.forName(s.next(), true, currentCL);
					bbd.addAnnotatedType(bm.createAnnotatedType(clazz));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void afterDeploymentValidation(AfterDeploymentValidation adv,
			BeanManager bm) {

		Set<Bean<?>> beans = bm.getBeans(Plugin.class,
				new AnnotationLiteral<Any>() {
				});
		if (beans.size() > 0) {
			Bean<?> bean = beans.iterator().next();
			bm.getReference(bean, Plugin.class,
					bm.createCreationalContext(bean));
		} else {
			System.out.println("No plugins present ");
		}

	}

}
