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
package org.apache.ode.cli;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;

import javax.management.JMX;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.ode.api.Platform;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(separators = "=", commandDescription = "Starts a new instance of an installed executable on the machine")
public class StartCommand extends AbstractCommand {

	public StartCommand(Connection connection, Formatter out) {
		super(connection, out);
	}

	@Parameter(names = "--name", description = "Specify the program name")
	public String name;

	@Parameter(names = "--target", description = "Specify installation target all <cluster|target>:<name>")
	public List<String> targets;

	@Override
	public void execute() throws IOException, MalformedObjectNameException {
		Platform platform = JMX.newMXBeanProxy(connection.getConnection(), ObjectName.getInstance(Platform.OBJECTNAME), Platform.class);
		String[] target = null;
		if (targets != null) {

		}
		platform.stop(name, target);
		out.format("Sucessfully started program %s\n", name);

	}

}
