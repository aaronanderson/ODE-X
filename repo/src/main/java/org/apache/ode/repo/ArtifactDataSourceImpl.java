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
package org.apache.ode.repo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;
import javax.activation.MimeTypeParseException;
import javax.inject.Inject;

import org.apache.ode.spi.repo.Artifact;
import org.apache.ode.spi.repo.ArtifactDataSource;

public class ArtifactDataSourceImpl implements ArtifactDataSource {

	private ArtifactImpl artifact;
	/*
	 * From activation spec: Also note that the class that implements the
	 * DataSource interface is responsible for typing the data.
	 */

	@Inject
	RepoFileTypeMap fileTypes;
	@Inject
	RepoCommandMap commandMap;

	@Override
	public String getContentType() {
		return artifact.getContentType();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return new ByteArrayInputStream(artifact.getContent());
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return new ByteArrayOutputStream() {

			@Override
			public void close() throws IOException {
				super.close();
				artifact.setContent(this.toByteArray());
			}

		};
	}

	@Override
	public void configure(Artifact artifact) throws MimeTypeParseException {
		this.artifact = (ArtifactImpl) artifact;// consider cloning
	}

	@Override
	public void configure(byte[] content, String fileName) throws MimeTypeParseException {
		this.artifact = new ArtifactImpl();
		artifact.setContent(content);
		String mimeType = fileTypes.getContentType(fileName);
		if (mimeType != null) {
			artifact.setContentType(mimeType);

		} else {
			throw new MimeTypeParseException(String.format("Unable to identify supported mime type for file %s", fileName));
		}
	}

	@Override
	public byte[] getContent() {
		return artifact.getContent();
	}

}
