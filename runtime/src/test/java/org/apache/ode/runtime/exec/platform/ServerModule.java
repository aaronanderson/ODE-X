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
package org.apache.ode.runtime.exec.platform;

import org.apache.ode.runtime.exec.platform.Cluster.ClusterId;
import org.apache.ode.runtime.exec.platform.Cluster.NodeId;

import com.google.inject.AbstractModule;

public class ServerModule extends AbstractModule {
	String clusterId;
	String nodeId;
	
	public ServerModule(String clusterId, String nodeId){
		this.clusterId=clusterId;
		this.nodeId=nodeId;
	}
	
	protected void configure() {
		bind(String.class).annotatedWith(NodeId.class).toInstance(nodeId);
		bind(String.class).annotatedWith(ClusterId.class).toInstance(clusterId);
		install(new JPAModule());
		install(new RepoModule());
		install(new JMSModule());
		install(new ScopeModule());
		install(new PlatformModule());
		install(new JMXModule());
	}

}
