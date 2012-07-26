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
package org.apache.ode.runtime.exec.platform.task;

import org.apache.ode.runtime.exec.cluster.xml.InstallProgramInput;
import org.apache.ode.runtime.exec.cluster.xml.InstallProgramOutput;
import org.apache.ode.spi.exec.task.TaskActionTransaction;
import org.apache.ode.spi.exec.task.TaskActionContext;
import org.apache.ode.spi.exec.task.TaskActionExec;

public class InstallAction implements TaskActionExec<InstallProgramInput, InstallProgramOutput>, TaskActionTransaction {

	@Override
	public void start(TaskActionContext ctx, InstallProgramInput input) {

	}

	@Override
	public void execute() {

	}

	@Override
	public InstallProgramOutput finish() {

		return null;
	}

	@Override
	public void complete() {

	}

}
