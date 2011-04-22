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
package org.apache.ode.bpel.plugin;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.ode.bpel.repo.BPELValidation;
import org.apache.ode.repo.CommandInfo;
import org.apache.ode.repo.RepoCommandMap;
import org.apache.ode.repo.RepoFileTypeMap;
import org.apache.ode.spi.Plugin;

@Named("BPELPlugin")
public class BPELPlugin implements Plugin {
	
	public static String BPEL_MIMETYPE= "application/bpel";
	//@Inject WSDLPlugin wsdlPlugin;
	@Inject RepoFileTypeMap fileTypes;
	@Inject RepoCommandMap commandMap;
	@Inject Provider<BPELValidation> validateProvider;
	
	@PostConstruct
	public void init(){
		System.out.println("Initializing BPELPlugin");
		fileTypes.registerExtension("bpel", BPEL_MIMETYPE);
		commandMap.registerCommandInfo(BPEL_MIMETYPE,new CommandInfo<BPELValidation>("validate",BPELValidation.class.getName(),true,validateProvider));
		System.out.println("BPELPlugin Initialized");
		
	}
	
	

}
