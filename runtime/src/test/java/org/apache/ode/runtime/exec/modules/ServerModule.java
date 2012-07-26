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
package org.apache.ode.runtime.exec.modules;

import org.apache.ode.runtime.exec.modules.AQJMSModule.AQVMJMSModule;
import org.apache.ode.runtime.exec.platform.NodeImpl.ClusterId;
import org.apache.ode.runtime.exec.platform.NodeImpl.NodeId;

import com.google.inject.AbstractModule;

public class ServerModule extends AbstractModule {
	String clusterId;
	String nodeId;

	public ServerModule(String clusterId, String nodeId) {
		this.clusterId = clusterId;
		this.nodeId = nodeId;
	}

	protected void configure() {
		bindConstant().annotatedWith(NodeId.class).to(nodeId);
		bindConstant().annotatedWith(ClusterId.class).to(clusterId);
		install(new JPAModule());
		install(new RepoModule());
		install(new JMSModule());
		install(new ScopeModule());
		install(new NodeModule());
		install(new JMXModule());
	}

	public static class VMServerModule extends AbstractModule {
		String aqBrokerURL;
		String clusterId;
		String nodeId;

		public VMServerModule(String aqBrokerURL, String clusterId, String nodeId) {
			this.aqBrokerURL = aqBrokerURL;
			this.clusterId = clusterId;
			this.nodeId = nodeId;
		}

		protected void configure() {
			bindConstant().annotatedWith(NodeId.class).to(nodeId);
			bindConstant().annotatedWith(ClusterId.class).to(clusterId);
			install(new JPAModule());
			install(new RepoModule());
			install(new AQVMJMSModule(aqBrokerURL));
			install(new ScopeModule());
			install(new NodeModule());
			install(new JMXModule());
		}

	}

}
