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
package org.apache.ode.api;

import java.beans.ConstructorProperties;
import java.io.IOException;

import javax.management.MXBean;

@MXBean
public interface Repository {

	public static final String OBJECTNAME = "org.apache.ode:type=Machine.Repository";

	public ArtifactId importArtifact(ArtifactId artifactId, String fileName, boolean overwrite, boolean noValidate, byte[] contents) throws IOException;

	public void refreshArtifact(ArtifactId artifactId, boolean noValidate, byte[] contents) throws IOException;

	public byte[] exportArtifact(ArtifactId artifact) throws IOException;

	public void removeArtifact(ArtifactId artifact) throws IOException;
	
	public String[] listContentTypes();

	public ArtifactId[] listArtifacts(String contentType, int resultLimit);

	public static class ArtifactId {

		public String getType() {
			return type;
		}

		public String getName() {
			return name;
		}

		public String getVersion() {
			return version;
		}

		@ConstructorProperties({ "name", "type", "version" })
		public ArtifactId(String name, String type, String version) {
			this.type = type;
			this.name = name;
			this.version = version;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("ArtifactId: name: ");
			sb.append(name);
			sb.append(" type: ");
			sb.append(type);
			sb.append(" version: ");
			sb.append(version);
			return sb.toString();
		}

		private final String type;
		private final String name;
		private final String version;

	}

}
