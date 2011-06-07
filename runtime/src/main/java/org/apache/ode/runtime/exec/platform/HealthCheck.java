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
package org.apache.ode.runtime.exec.platform;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.ode.spi.exec.Action;
import org.apache.ode.spi.exec.Action.ActionId;
import org.apache.ode.spi.exec.ActionStatus;
import org.apache.ode.spi.exec.PlatformException;
import org.apache.ode.spi.exec.Target;

public class HealthCheck implements Runnable {

	@PersistenceContext(unitName = "platform")
	private EntityManager pmgr;

	org.apache.ode.runtime.exec.cluster.xml.HealthCheck config;

	@Override
	public void run() {

	}

	public void setConfig(org.apache.ode.runtime.exec.cluster.xml.HealthCheck config) {
		this.config = config;
	}

}
