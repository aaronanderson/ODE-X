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
package org.apache.ode.eclipse.runtime.bpel;


import java.io.File;

import org.eclipse.bpel.runtimes.publishers.GenericBPELPublisher;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jst.server.core.PublishUtil;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleArtifact;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.ModuleDelegate;


public class ODEBPELPublisher extends GenericBPELPublisher {

	public ODEBPELPublisher() {
		super();
	}

	@Override
	public IStatus[] publish(IModuleArtifact[] artifacts, IProgressMonitor monitor) {
		// resources will always be null for some weird reason :(
		// therefore we generate a BPELModuleArtifact
		// the module id value enables us to get BPEL file path relative to its project
		IModule[] modules = super.getModule();
		
		try {
			IModule last = modules[modules.length-1];
			IPath root = createDeploymentDestination(last);
			ModuleDelegate delegate = (ModuleDelegate)last.loadAdapter(ModuleDelegate.class, new NullProgressMonitor());
			IModuleResource[] resources = delegate.members();
			PublishUtil.publishFull(resources, root, monitor);
		} catch(  CoreException ce ) {
			// TODO return bad status
		}
		return new IStatus[]{Status.OK_STATUS};

	}

	@Override
	public IStatus[] unpublish(IProgressMonitor monitor) {
		IModule[] modules = super.getModule();
		IModule last = modules[modules.length - 1];
		IStatus[] result = new Status[modules.length];
		IPath root = createDeploymentDestination(last);
		PublishUtil.deleteDirectory(root.toFile(), monitor);
		return result;
	}

	/**
	 * This method will create a folder inside the WEB-INF\processes subfolder
	 * of the ODE installation
	 */
	protected IPath createDeploymentDestination(IModule module) {
		String moduleName = module.getName();
		String deployAppName = moduleName;

		// get TOMCAT_HOME
		IRuntime serverDef = getServerRuntime().getRuntime();
		IPath tomcatHome = serverDef.getLocation();

		// append ODE's Process target Dir tomcatHome
		IPath deployTarget = tomcatHome.append("webapps").append("ode")
				.append("WEB-INF").append("processes").append(deployAppName);

		File f = deployTarget.toFile();
		if (!f.exists()) {
			f.mkdir();
		}

		return deployTarget;
	}
}
