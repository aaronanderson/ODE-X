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
import javax.inject.Singleton;

import org.apache.ode.server.JMXServer;
import org.apache.ode.server.WebServer;

@Singleton
public class WebServerHandler extends Handler {
	CreationalContext<WebServer> webServerCtx;
	CreationalContext<JMXServer> jmxServerCtx;
	Bean<WebServer> webserverBean;
	Bean<JMXServer> jmxServerBean;
	WebServer webServer;
	JMXServer jmxServer;

	public void beforeBeanDiscovery(BeforeBeanDiscovery bbd, BeanManager bm) {
		bbd.addAnnotatedType(bm.createAnnotatedType(WebServer.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(JMXServer.class));
	}

	@Override
	public void afterDeploymentValidation(AfterDeploymentValidation adv, BeanManager bm) {

		Set<Bean<?>> beans = bm.getBeans(WebServer.class, new AnnotationLiteral<Any>() {
		});
		if (beans.size() > 0) {
			webserverBean = (Bean<WebServer>)beans.iterator().next();
			webServerCtx = bm.createCreationalContext(webserverBean);
			webServer = (WebServer)bm.getReference(webserverBean, WebServer.class, webServerCtx);
		} else {
			System.out.println("Can't find class " + WebServer.class);
		}
		beans = bm.getBeans(JMXServer.class, new AnnotationLiteral<Any>() {
		});
		if (beans.size() > 0) {
			jmxServerBean = (Bean<JMXServer>)beans.iterator().next();
			jmxServerCtx = bm.createCreationalContext(jmxServerBean);
			jmxServer = (JMXServer)bm.getReference(jmxServerBean, JMXServer.class, jmxServerCtx);
		} else {
			System.out.println("Can't find class " + JMXServer.class);
		}

	}

	@Override
	public void beforeShutdown(BeforeShutdown adv, BeanManager bm) {
	  System.out.println("releasing contexts");
	  if (webServer!=null){
		  webserverBean.destroy(webServer, webServerCtx);
	  }
	  if (jmxServer!=null){
		  jmxServerBean.destroy(jmxServer, jmxServerCtx);
	  }
	}

}
