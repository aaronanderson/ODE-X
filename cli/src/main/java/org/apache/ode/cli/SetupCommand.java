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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Formatter;

import javax.management.JMX;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.ode.api.Platform;
import org.apache.ode.api.Repository.ArtifactId;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(separators = "=", commandDescription = "Retrieves install data for installation")
public class SetupCommand extends AbstractCommand {

	public SetupCommand(Connection connection, Formatter out) {
		super(connection, out);
	}

	@Parameter(names = "--name", description = "Executable name", required = true)
	public String name;

	@Parameter(names = "--version", description = "Executable version")
	public String version;

	@Parameter(names = "--file", description = "File to save install data to", converter = FileConverter.class)
	public File file;

	@Override
	public void execute() throws IOException, MalformedObjectNameException {
		Platform platform = JMX.newMXBeanProxy(connection.getConnection(), ObjectName.getInstance(Platform.OBJECTNAME), Platform.class);
		ArtifactId id = new ArtifactId(name, Platform.EXEC_MIMETYPE, version);
		byte[] contents = platform.setup(id);
		if (contents != null) {
			if (file != null) {
				FileOutputStream fos = new FileOutputStream(file);
				FileChannel channel = fos.getChannel();
				ByteBuffer bb = ByteBuffer.wrap(contents);
				channel.write(bb);
				channel.close();
				out.format("Sucessfully saved executable %s install data to file %s\n", id, file.getAbsolutePath());
			} else {
				out.format("Sucessfully retrieved executable %s install data: \n\t %s\n", id, new String(contents));
			}
		} else {
			out.format("No installation data available for %s \n", id);
		}

	}
}
