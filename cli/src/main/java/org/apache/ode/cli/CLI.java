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
package org.apache.ode.cli;

import java.util.Formatter;

import com.beust.jcommander.JCommander;

public class CLI {
	
	public static boolean execute( StringBuilder output, String ... args){
		Connection con = new Connection();
		JCommander jc = new JCommander(con);
		
		Formatter out = new Formatter(output);
		ImportCommand importCmd = new ImportCommand(con,out);
		jc.addCommand("import", importCmd);
		ExportCommand exportCmd = new ExportCommand(con,out);
		jc.addCommand("export", exportCmd);
		ListTypesCommand listTypesCmd = new ListTypesCommand(con,out);
		jc.addCommand("listTypes", listTypesCmd);
		ListArtifactsCommand listArtifactsCmd = new ListArtifactsCommand(con,out);
		jc.addCommand("list", listArtifactsCmd);
		InstallCommand installCmd = new InstallCommand(con,out);
		jc.addCommand("install", installCmd);
		UninstallCommand uninstallCmd = new UninstallCommand(con,out);
		jc.addCommand("uninstall", uninstallCmd);
		StartCommand startCmd = new StartCommand(con,out);
		jc.addCommand("start", startCmd);
		StopCommand stopCmd = new StopCommand(con,out);
		jc.addCommand("stop", stopCmd);

		jc.parse(args);
		if (jc.getParsedCommand() != null) {
			JCommander subCommand = jc.getCommands().get(jc.getParsedCommand());
			for (Object o : subCommand.getObjects()) {
				if (o instanceof AbstractCommand) {
					try{
					((AbstractCommand)o).execute();
					return true;
					}catch (Exception e){
						output.append(e.getMessage());
					}
				}
			}
		}else{
			jc.usage(output);
		}
		return false;
	}

	public static void main(String[] args) {
		StringBuilder out = new StringBuilder();
		execute(out,args);
		System.out.print(out.toString());
	}

}
