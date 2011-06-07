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

import java.awt.datatransfer.DataFlavor;
import java.io.IOException;

import javax.activation.CommandObject;
import javax.activation.MimeTypeParseException;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.xml.namespace.QName;

import org.apache.ode.spi.repo.Artifact;
import org.apache.ode.spi.repo.ArtifactDataSource;
import org.apache.ode.spi.repo.DataContentHandler;
import org.apache.ode.spi.repo.DataHandler;
import org.apache.ode.spi.repo.Repository;
import org.apache.ode.spi.repo.RepositoryException;

public class RepositoryImpl implements Repository {

	@PersistenceContext(unitName = "repo")
	private EntityManager mgr;

	@Inject
	RepoFileTypeMap fileTypes;
	@Inject
	RepoCommandMap commandMap;
	@Inject
	Provider<ArtifactDataSourceImpl> dsProvider;

	@Override
	public <C> void create(QName qname, String contentType, String version, C content) throws RepositoryException {
		ArtifactImpl artifact = null;
		if (content instanceof ArtifactDataSourceImpl) {
			artifact = ((ArtifactDataSourceImpl) content).artifact;
			if (mgr.contains(artifact)) {
				throw new RepositoryException("ArtifactDataSource already attached to an existing artifact");
			}
			artifact.setQName(qname);
			artifact.setVersion(version);
		} else {
			artifact = new ArtifactImpl();
			artifact.setQName(qname);
			artifact.setVersion(version);
			artifact.setContentType(contentType);
			if (content instanceof byte[]) {
				artifact.setContent((byte[])content);
			} else {
				DataHandler dh = getDataHandler(content, contentType);
				try {
					artifact.setContent(dh.toContent());
				} catch (IOException e) {
					throw new RepositoryException(e);
				}
			}
		}
		mgr.getTransaction().begin();
		mgr.persist(artifact);
		mgr.getTransaction().commit();
	}

	protected ArtifactImpl loadArtifact(QName qname, String contentType, String version) throws RepositoryException {
		Query q = mgr.createNamedQuery("lookupArtifact");
		q.setParameter("qname", qname.toString());
		q.setParameter("type", contentType);
		q.setParameter("version", version);

		try {
			return (ArtifactImpl) q.getSingleResult();
		} catch (NoResultException nr) {
			throw new RepositoryException(String.format("Artifact qname %s contentType %s version %s does not exists", qname, contentType, version), nr);
		}

	}

	@Override
	public <C> C read(QName qname, String contentType, String version, Class<C> javaType) throws RepositoryException {
		ArtifactImpl artifact = loadArtifact(qname, contentType, version);

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
	public <C> void update(QName qname, String contentType, String version, C content) throws RepositoryException {
		ArtifactImpl artifact = null;
		if (content instanceof ArtifactDataSourceImpl) {
			ArtifactDataSourceImpl ds = (ArtifactDataSourceImpl) content;
			artifact = ds.artifact;
			if (!mgr.contains(artifact)) {
				artifact = loadArtifact(qname, contentType, version);
			}
			artifact.setContent(ds.getContent());
		} else if (content instanceof byte[]) {
			artifact = loadArtifact(qname, contentType, version);
			artifact.setContent((byte[]) content);
		} else {
			artifact = loadArtifact(qname, contentType, version);
			DataHandler dh = getDataHandler(content, contentType);
			try {
				artifact.setContent(dh.toContent());
			} catch (IOException e) {
				throw new RepositoryException(e);
			}
		}

		mgr.getTransaction().begin();
		mgr.merge(artifact);
		mgr.getTransaction().commit();
	}

	@Override
	public void delete(QName qname, String contentType, String version) throws RepositoryException {
		ArtifactImpl artifact = null;
		Query q = mgr.createNamedQuery("lookupArtifact");
		q.setParameter("qname", qname.toString());
		q.setParameter("type", contentType);
		q.setParameter("version", version);

		try {
			artifact = (ArtifactImpl) q.getSingleResult();
		} catch (NoResultException nr) {
			throw new RepositoryException(String.format("Artifact qname %s contentType %s version %s does not exists", qname, contentType, version), nr);
		}
		mgr.getTransaction().begin();
		mgr.remove(artifact);
		mgr.getTransaction().commit();
	}

	@Override
	public boolean exists(QName qname, String contentType, String version) throws RepositoryException {
		Query q = mgr.createNamedQuery("artifactExists");
		q.setParameter("qname", qname.toString());
		q.setParameter("type", contentType);
		q.setParameter("version", version);

		Long count = (Long) q.getSingleResult();
		if (count.intValue() > 0) {
			return true;
		} else {
			return false;
		}
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
