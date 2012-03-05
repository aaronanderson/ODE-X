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

@Parameters(separators = "=", commandDescription = "Stops a running instance of an installed executable on the machine")
public class StopCommand extends AbstractCommand{
   
  public StopCommand(Connection connection, Formatter out) {
		super(connection, out);
	}

@Parameter(names = "--instancename", description = "Specify the name of the new instance", required=true)
  public String instanceName;

@Override
public void execute()  throws IOException, MalformedObjectNameException{
	// TODO Auto-generated method stub
	
}

}
