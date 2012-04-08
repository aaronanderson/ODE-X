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

import org.apache.ode.api.Repository;
import org.apache.ode.api.Repository.ArtifactId;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(separators = "=", commandDescription = "Imports a file into the repository")
public class ImportCommand extends AbstractCommand {

	public ImportCommand(Connection connection, Formatter out) {
		super(connection, out);
	}

	@Parameter(names = "--file", description = "The file to import", converter = FileConverter.class, required = true)
	public File file;

	@Parameter(names = "--type", description = "Override the artifact type")
	public String type;

	@Parameter(names = "--name", description = "Override the artifact name")
	public String name;

	@Parameter(names = "--version", description = "Specify the artifact version")
	public String version;

	@Parameter(names = "--overwrite", description = "Overwrite the artifact if it exists")
	public boolean overwrite = false;

	@Parameter(names = "--novalidate", description = "Do not validate the artifact on import")
	public boolean noValidate = false;

	@Override
	public void execute() throws IOException, MalformedObjectNameException {
		Repository repo = JMX.newMXBeanProxy(connection.getConnection(), ObjectName.getInstance(Repository.OBJECTNAME), Repository.class);
		FileInputStream fis = new FileInputStream(file);
		FileChannel channel = fis.getChannel();
		ByteBuffer bb = ByteBuffer.allocate((int) channel.size());
		channel.read(bb);
		byte[] contents = bb.array();
		channel.close();
		ArtifactId id = new ArtifactId(name, type, version);
		id = repo.importArtifact(id, file.getName(), overwrite, noValidate, contents);
		out.format("Sucessfully imported file %s\n", file.getAbsoluteFile());
		out.format("\t%s\n", id);
	}
}
