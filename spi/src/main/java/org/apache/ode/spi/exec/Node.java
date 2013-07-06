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
package org.apache.ode.spi.exec;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.ode.spi.exec.Component.ExecutableSet;
import org.apache.ode.spi.exec.bond.Reactor;
import org.apache.ode.spi.exec.task.TaskActionDefinition;
import org.apache.ode.spi.exec.task.TaskDefinition;

public interface Node {

	public static final String CLUSTER_MIMETYPE = "application/ode-cluster";
	public static final String CLUSTER_NAMESPACE = "http://ode.apache.org/cluster";

	public static final String NODE_MQ_PROP_CLUSTER = "ODE_CLUSTER";
	public static final String NODE_MQ_PROP_NODE = "ODE_NODE";
	public static final String NODE_MQ_PROP_TASKID = "ODE_TASKID";
	public static final String NODE_MQ_PROP_ACTIONID = "ODE_ACTIONID";

	public static final String NODE_MQ_CORRELATIONID_TASK = "ODE_TASK_%s";
	public static final String NODE_MQ_CORRELATIONID_ACTION = "ODE_ACTION_%s";

	public static final String NODE_MQ_PROP_VALUE_NEW = "NEW";

	public static final String NODE_MQ_FILTER_NODE = "ODE_NODE='%s'";
	public static final String NODE_MQ_FILTER_CLUSTER = "ODE_CLUSTER='%s'";
	public static final String NODE_MQ_FILTER_TASK = "ODE_NODE='%s' AND ODE_TASKID IS NOT NULL";
	public static final String NODE_MQ_FILTER_TASK_ACTION = "ODE_NODE='%s' AND ODE_ACTIONID IS NOT NULL";
	public static final String NODE_MQ_FILTER_TASK_AND_TASK_ACTION = "ODE_NODE='%s' AND (ODE_ACTIONID IS NOT NULL OR ODE_ACTIONID IS NOT NULL)";

	public static final String NODE_MQ_NAME_HEALTHCHECK = "ODE_HEALTHCHECK";
	public static final String NODE_MQ_NAME_TASK = "ODE_TASK";
	public static final String NODE_MQ_NAME_MESSAGE = "ODE_MESSAGE";

	public QName architecture();

	public Set<QName> getComponents();

	public void registerComponent(Component component);

	public void unregisterComponent(Component component);

	public Map<QName, ExecutableSet> getInstructionSets();

	public Map<QName, TaskDefinition<?, ?>> getTaskDefinitions();

	public Map<QName, TaskActionDefinition<?, ?>> getTaskActionDefinitions();

	public void online() throws PlatformException;

	public void offline() throws PlatformException;
	
	public Reactor reactor(URI execution);
	
	
}
