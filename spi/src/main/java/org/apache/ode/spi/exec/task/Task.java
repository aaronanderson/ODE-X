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
package org.apache.ode.spi.exec.task;

import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.ode.spi.exec.Message;
import org.apache.ode.spi.exec.target.Target;
import org.w3c.dom.Document;

public interface Task {

	public static enum TaskState {
		SUBMIT, START, EXECUTE, CANCEL, FINISH, COMPLETE, FAIL
	}

	public static interface TaskId {

	}

	public String nodeId();

	public void refresh();

	public QName name();

	public TaskId id();

	public QName component();

	public TaskState state();

	public Set<Target> targets();

	public List<Message> messages();

	public Set<TaskAction> actions();

	public Document input();

	public Document output();

	public Date start();

	public Date finish();

	public Date modified();

	

	//TODO support external TaskActions, where action actually performed outside ODE framework but action updates and messages stil managed by ODE


}