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

import java.io.IOException;
import java.util.Formatter;

import javax.management.MalformedObjectNameException;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(separators = "=", commandDescription = "Exports a file from the repository")
public class ExportCommand extends AbstractCommand{
 
  public ExportCommand(Connection connection, Formatter out) {
		super(connection, out);
	}

@Parameter(names = "--file", description = "The file to export to", required = true)
  public String file;
  
  @Parameter(names = "--type", description = "Override the type", required = true)
  public String type;
  
  @Parameter(names = "--name", description = "Override the name", required = true)
  public String name;
  
  @Parameter(names = "--version", description = "Override the version")
  public String version;

@Override
public void execute() throws IOException, MalformedObjectNameException {
	// TODO Auto-generated method stub
	
}
}
