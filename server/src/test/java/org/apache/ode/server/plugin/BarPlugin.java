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
package org.apache.ode.server.plugin;

import java.io.File;
import java.io.IOException;

import javax.activation.CommandObject;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.ode.spi.Plugin;
import org.apache.ode.spi.repo.CommandInfo;
import org.apache.ode.spi.repo.RepoCommandMap;
import org.apache.ode.spi.repo.RepoFileTypeMap;

@Named("BarPlugin")
public class BarPlugin implements Plugin {
	
	public static String BAR_MIMETYPE= "application/bar";
	//@Inject WSDLPlugin wsdlPlugin;
	@Inject RepoFileTypeMap fileTypes;
	@Inject RepoCommandMap commandMap;
	@Inject Provider<BarValidation> barProvider;
	
	@PostConstruct
	public void init(){
		System.out.println("Initializing BPELPlugin");
		fileTypes.registerExtension("bar", BAR_MIMETYPE);
		fileTypes.registerExtension("bar2", BAR_MIMETYPE);
		commandMap.registerCommandInfo(BAR_MIMETYPE,new CommandInfo<BarValidation>("validate",BarValidation.class.getName(),true,barProvider));
		System.out.println("BPELPlugin Initialized");
		
	}
	

	public String getContentType(String fileName ){
		return fileTypes.getContentType(fileName);
	}

	public BarValidation getCommand(File f, String command ) throws Exception{
		FileDataSource fds = new FileDataSource(f);
		fds.setFileTypeMap(fileTypes);
		DataHandler handler = new DataHandler(fds);
		handler.setCommandMap(commandMap);
		javax.activation.CommandInfo info = handler.getCommand(command);
		if (info == null){
			return null;
		}
		return (BarValidation)info.getCommandObject(handler, getClass().getClassLoader());
	}
	
	static public class BarValidation implements CommandObject{

		@Override
		public void setCommandContext(String command, DataHandler handler)
				throws IOException {
			
		}
	}
 
}
