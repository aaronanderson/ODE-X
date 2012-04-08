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

import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.ode.spi.exec.ActionTask.ActionId;
import org.apache.ode.spi.exec.ActionTask.ActionStatus;
import org.apache.ode.spi.exec.Component.InstructionSet;
import org.apache.ode.spi.repo.Artifact;
import org.w3c.dom.Document;

public interface Platform {

	public static final String EXEC_MIMETYPE = "application/ode-executable";
	public static final String EXEC_NAMESPACE = "http://ode.apache.org/executable";
	public static final String EXEC_CTX_NAMESPACE = "http://ode.apache.org/execution-context";
	public static final String PROGRAM_NAMESPACE = "http://ode.apache.org/program";

	public static final String PLATFORM_NAMESPACE = "http://ode.apache.org/platform";
	public static final QName PLATFORM = new QName(PLATFORM_NAMESPACE,"platform");
	
	public static final String ARCHITECTURE_NAMESPACE = "http://ode.apache.org/architecture";
	
	public static final QName EXEC_INSTRUCTION_SET_NAME = new QName(EXEC_NAMESPACE, "Executable");
	public static final InstructionSet EXEC_INSTRUCTION_SET = new InstructionSet(EXEC_INSTRUCTION_SET_NAME, "org.apache.ode.spi.exec.xml",org.apache.ode.spi.exec.xml.ObjectFactory.class, "org.apache.ode.spi.exec.instruction.xml",org.apache.ode.spi.exec.instruction.xml.ObjectFactory.class);


	public enum PlatformAction {

		INSTALL_ACTION(new QName(PLATFORM_NAMESPACE, "install")),ONLINE_ACTION(new QName(PLATFORM_NAMESPACE, "online")), START_ACTION(new QName(PLATFORM_NAMESPACE, "start")), STOP_ACTION(new QName(
				PLATFORM_NAMESPACE, "stop")),OFFLINE_ACTION(new QName(PLATFORM_NAMESPACE, "offline")), UNINSTALL_ACTION(new QName(PLATFORM_NAMESPACE, "uninstall"));

		private PlatformAction(QName qname) {
			this.qname = qname;
		}

		private QName qname;

		public QName qname() {
			return qname;
		}
	}
	
	public QName architecture();
	
	public void registerComponent(Component component);

	public Set<NodeStatus> status();

	public Document setup(Artifact ... executables) throws PlatformException;

	public void install(QName id, Document programConfiguration, Target... targets) throws PlatformException;

	public Program programInfo(QName id) throws PlatformException;

	public Process start(QName id, Target... targets) throws PlatformException;

	public void stop(QName id, Target... targets) throws PlatformException;

	public void uninstall(QName id, Target... targets) throws PlatformException;

	public ActionId execute(QName action, Document actionInput, Target... targets) throws PlatformException;

	public ActionStatus status(ActionId actionId) throws PlatformException;

	public void cancel(ActionId actionId) throws PlatformException;
}
