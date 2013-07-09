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
package org.apache.ode.data.core.repo;

import java.awt.datatransfer.DataFlavor;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import javax.activation.CommandObject;
import javax.activation.MimeTypeParseException;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.ode.spi.repo.Artifact;
import org.apache.ode.spi.repo.ArtifactDataSource;
import org.apache.ode.spi.repo.Criteria;
import org.apache.ode.spi.repo.DataContentHandler;
import org.apache.ode.spi.repo.DataHandler;
import org.apache.ode.spi.repo.Repository;
import org.apache.ode.spi.repo.RepositoryException;

@Singleton
public abstract class RepositoryBaseImpl implements Repository {

	@Inject
	protected RepoFileTypeMap fileTypes;
	@Inject
	protected RepoCommandMap commandMap;
	@Inject
	protected Provider<ArtifactDataSourceImpl> dsProvider;

	@Override
	public <C> UUID create(URI uri, String version, String contentType, C content) throws RepositoryException {
		Artifact artifact = null;

		if (content instanceof Artifact) {
			artifact = (Artifact) content;
			if (exists(new Criteria(artifact.getURI(), artifact.getContentType(), artifact.getVersion()))) {
				throw new RepositoryException("ArtifactDataSource already attached to an existing artifact");
			}
			artifact.setURI(uri);
			artifact.setVersion(version);
		} else if (content instanceof ArtifactDataSourceImpl) {
			artifact = ((ArtifactDataSourceImpl) content).getArtifact();
			if (exists(new Criteria(uri, contentType, version))) {
				throw new RepositoryException("ArtifactDataSource already attached to an existing artifact");
			}
			artifact.setURI(uri);
			artifact.setVersion(version);
		} else {
			artifact = new Artifact().withUUID(UUID.randomUUID()).withURI(uri).withVersion(version).withContentType(contentType);
			if (content instanceof byte[]) {
				artifact.setContent((byte[]) content);
			} else {
				DataHandler dh = getDataHandler(content, contentType);
				try {
					artifact.setContent(dh.toContent());
				} catch (IOException e) {
					throw new RepositoryException(e);
				}
			}
		}
		if (!artifact.getURI().isAbsolute()) {
			throw new RepositoryException(String.format("Artifact URI must be absolute %s", artifact.getURI()));
		}
		createArtifact(artifact);
		return artifact.getId();
	}

	protected abstract <R> Artifact loadArtifact(R criteria) throws RepositoryException;

	protected abstract void createArtifact(Artifact artifact) throws RepositoryException;

	protected abstract void storeArtifact(Artifact artifact) throws RepositoryException;

	@Override
	public <R, C> C read(R criteria, Class<C> javaType) throws RepositoryException {
		Artifact artifact = loadArtifact(criteria);

		if (Artifact.class.isAssignableFrom(javaType)) {
			return (C) artifact;
		}
		ArtifactDataSourceImpl ds = dsProvider.get();
		try {
			ds.configure(artifact);
		} catch (MimeTypeParseException e) {
			throw new RepositoryException(e);
		}
		if (ArtifactDataSource.class.isAssignableFrom(javaType)) {
			return (C) ds;
		}
		if (byte[].class.isAssignableFrom(javaType)) {
			return (C) ds.getContent();
		}
		DataHandler dh = getDataHandler(ds);
		DataFlavor[] flavors = dh.getTransferDataFlavors();
		C content = null;
		for (DataFlavor df : flavors) {
			if (df.getRepresentationClass().equals(javaType)) {
				try {
					content = (C) dh.getTransferData(df);
					break;
				} catch (Exception e) {
					throw new RepositoryException(e);
				}
			}
		}
		if (content == null) {
			throw new RepositoryException(String.format("Unable to create Java representation class %s", javaType));
		} else {
			return content;
		}
	}

	@Override
	public <R, C> void update(R criteria, C content) throws RepositoryException {
		Artifact artifact = loadArtifact(criteria);
		if (content instanceof Artifact) {
			artifact.setContent(((Artifact) content).getContent());
		} else if (content instanceof ArtifactDataSourceImpl) {
			ArtifactDataSourceImpl ds = (ArtifactDataSourceImpl) content;
			artifact.setContent(ds.getContent());
		} else if (content instanceof byte[]) {
			artifact.setContent((byte[]) content);
		} else {
			DataHandler dh = getDataHandler(content, artifact.getContentType());
			try {
				artifact.setContent(dh.toContent());
			} catch (IOException e) {
				throw new RepositoryException(e);
			}
		}

		storeArtifact(artifact);
	}

	@Override
	public <T extends CommandObject> void registerCommandInfo(String mimeType, String commandName, boolean preferred, Provider<T> provider) {
		commandMap.registerCommandInfo(mimeType, new CommandInfo<T>(commandName, null, true, provider));

	}

	@Override
	public void registerFileExtension(String fileExtension, String mimeType) {
		fileTypes.registerFileExtension(fileExtension, mimeType);
	}

	@Override
	public void registerNamespace(String namespace, String mimeType) {
		fileTypes.registerNamespace(namespace, mimeType);

	}

	@Override
	public void registerHandler(String mimeType, DataContentHandler handler) {
		commandMap.registerDataContentHandler(mimeType, handler);

	}

	@Override
	public DataHandler getDataHandler(ArtifactDataSource ds) {
		DataHandler dh = new DataHandler(ds);
		dh.setCommandMap(commandMap);
		return dh;
	}

	@Override
	public <T> DataHandler getDataHandler(T content, String mimeType) {
		DataHandler dh = new DataHandler(content, mimeType);
		dh.setCommandMap(commandMap);
		return dh;
	}

}
