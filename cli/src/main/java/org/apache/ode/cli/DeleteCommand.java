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

@Parameters(separators = "=", commandDescription = "Deletes an artifact the repository")
public class DeleteCommand extends AbstractCommand {

	public DeleteCommand(Connection connection, Formatter out) {
		super(connection, out);
	}

	@Parameter(names = "--type", description = "The contentType to delete", required = true)
	public String type;

	@Parameter(names = "--name", description = "The QName to delete", required = true)
	public String name;

	@Parameter(names = "--version", description = "The version to delete", required = true)
	public String version;

	@Override
	public void execute() throws IOException, MalformedObjectNameException {
		Repository repo = JMX.newMXBeanProxy(connection.getConnection(), ObjectName.getInstance(Repository.OBJECTNAME), Repository.class);
		ArtifactId id = new ArtifactId(name, type, version);
		repo.removeArtifact(id);
		out.format("Sucessfully deleted artifact %s\n", id);
	}
}
