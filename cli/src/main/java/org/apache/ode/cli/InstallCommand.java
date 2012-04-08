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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Formatter;
import java.util.List;

import javax.management.JMX;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.ode.api.Platform;
import org.apache.ode.api.Repository.ArtifactId;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(separators = "=", commandDescription = "Installs an executable onto the platform")
public class InstallCommand extends AbstractCommand {

	public InstallCommand(Connection connection, Formatter out) {
		super(connection, out);
	}

	@Parameter(names = "--id", description = "Program name")
	public String id;

	@Parameter(names = "--name", description = "Executable name", required = true)
	public String name;

	@Parameter(names = "--version", description = "Executable version")
	public String version;

	@Parameter(names = "--file", description = "Install data file to for install", converter = FileConverter.class)
	public File file;

	@Parameter(names = "--target", description = "Specify installation target all <cluster|target>:<name>")
	public List<String> targets;

	@Override
	public void execute() throws IOException, MalformedObjectNameException {
		Platform platform = JMX.newMXBeanProxy(connection.getConnection(), ObjectName.getInstance(Platform.OBJECTNAME), Platform.class);
		byte[] contents = null;
		if (file != null) {
			FileInputStream fis = new FileInputStream(file);
			FileChannel channel = fis.getChannel();
			ByteBuffer bb = ByteBuffer.allocate((int) channel.size());
			channel.read(bb);
			contents = bb.array();
			channel.close();
		}
		if (id == null) {
			id = name;
		}
		version = version != null ? version : "1.0";
		ArtifactId aid = new ArtifactId(name, Platform.EXEC_MIMETYPE, version);
		String[] target = null;
		if (targets != null) {

		}
		platform.install(id, aid, contents, target);
		out.format("Sucessfully installed executable %s with program name %s\n", id, name);

	}
}
