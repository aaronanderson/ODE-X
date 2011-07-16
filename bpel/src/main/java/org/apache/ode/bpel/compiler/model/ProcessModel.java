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
package org.apache.ode.bpel.compiler.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.ode.bpel.exec.xml.Process;

public class ProcessModel extends ActivityModel {

	final Process startProcess;
	final Process endProcess;
	List<Import> imports = new ArrayList<Import>();

	public ProcessModel() {
		startProcess = new Process();
		endProcess = new Process();
	}

	public Process getStartProcess() {
		return startProcess;
	}

	public Process getEndProcess() {
		return endProcess;
	}

	public static class Import {

	}

	public static class PartnerLink {

	}

}
