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

import javax.management.JMX;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.ode.api.BuildSystem;
import org.apache.ode.api.Repository.ArtifactId;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(separators = "=", commandDescription = "Builds an executable from a build plan in the repository")
public class BuildCommand extends AbstractCommand {

	public BuildCommand(Connection connection, Formatter out) {
		super(connection, out);
	}

	@Parameter(names = "--file", description = "The build plan to build", converter = FileConverter.class)
	public File file;

	@Parameter(names = "--type", description = "The contentType of the build plan in the repository")
	public String type;

	@Parameter(names = "--name", description = "The QName to of the build plan in the repository")
	public String name;

	@Parameter(names = "--version", description = "The version of the build plan in the repository")
	public String version;

	@Override
	public void execute() throws IOException, MalformedObjectNameException {
		BuildSystem bsys = JMX.newMXBeanProxy(connection.getConnection(), ObjectName.getInstance(BuildSystem.OBJECTNAME), BuildSystem.class);
		if (file != null) {
			FileInputStream fis = new FileInputStream(file);
			FileChannel channel = fis.getChannel();
			ByteBuffer bb = ByteBuffer.allocate((int) channel.size());
			channel.read(bb);
			byte[] contents = bb.array();
			channel.close();
			bsys.build(contents);
			out.format("Sucessfully built plan %s\n", file.getAbsolutePath());
		} else {
			type = type != null ? type : BuildSystem.BUILDPLAN_MIMETYPE;
			version = version != null ? version : "1.0";
			ArtifactId id = new ArtifactId(name, type, version);
			bsys.build(id);
			out.format("Sucessfully built artifact %s\n", id);
		}
	}
}
