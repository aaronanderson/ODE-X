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

import java.util.logging.Logger;

import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.inject.Singleton;

import org.apache.ode.server.JMXServer;
import org.apache.ode.server.WebServer;
import org.apache.ode.spi.cdi.Handler;

@Singleton
public class WebServerHandler extends Handler {
	
	private static final Logger log = Logger.getLogger(WebServerHandler.class.getName());

	public void beforeBeanDiscovery(BeforeBeanDiscovery bbd, BeanManager bm) {
		bbd.addAnnotatedType(bm.createAnnotatedType(WebServer.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(JMXServer.class));
	}

	@Override
	public void afterDeploymentValidation(AfterDeploymentValidation adv, BeanManager bm) {
		manage(WebServer.class);
		manage(JMXServer.class);
	}

	@Override
	public void beforeShutdown(BeforeShutdown adv, BeanManager bm) {
		stop();
	}

}
