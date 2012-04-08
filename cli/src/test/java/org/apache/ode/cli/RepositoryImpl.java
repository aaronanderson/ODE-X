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

import org.apache.ode.api.Repository;
import org.apache.ode.api.Repository.ArtifactId;

public class RepositoryImpl implements Repository {

	@Override
	public ArtifactId importArtifact(ArtifactId artifactId, String fileName, boolean overwrite, boolean noValidate, byte[] contents) throws IOException {
		return null;
	}

	@Override
	public void refreshArtifact(ArtifactId artifactId, boolean noValidate, byte[] contents) throws IOException {
	}

	@Override
	public byte[] exportArtifact(ArtifactId artifact) throws IOException {
		return null;
	}

	@Override
	public void removeArtifact(ArtifactId artifact) throws IOException {
	}

	@Override
	public String[] listContentTypes() {
		return new String[0];
	}

	@Override
	public ArtifactId[] listArtifacts(String contentType, int resultLimit) {
		return new ArtifactId[0];
	}

}
