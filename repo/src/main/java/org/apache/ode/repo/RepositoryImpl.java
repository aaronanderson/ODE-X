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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Observable;

import javax.activation.CommandObject;
import javax.activation.MimeTypeParseException;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.xml.namespace.QName;

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

	public <C> C load(QName qname, String version, String contentType, Class<C> javaType) throws RepositoryException {
		Query q = mgr.createNamedQuery("lookupArtifact");
		q.setParameter("qname", qname.toString());
		q.setParameter("type", contentType);
		q.setParameter("version", version);

		ArtifactImpl artifact;
		try{
		   artifact = (ArtifactImpl) q.getSingleResult();
		}catch  (NoResultException nr){
			throw new RepositoryException(String.format("Artifact qname %s contentType %s version %s does not exists", qname, contentType, version),nr);
		}
		
		ArtifactDataSourceImpl ds = new ArtifactDataSourceImpl();
		try {
			ds.configure(artifact);
		} catch (MimeTypeParseException e) {
			throw new RepositoryException(e);
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

	/*
	 * DataHandler load(QName qname, String version, String type){
	 * 
	 * }
	 */

	public <C> void store(QName qname, String version, String contentType, C content) throws RepositoryException {
		// Check if artifact already exists
		Query q = mgr.createNamedQuery("uniqueArtifact");
		q.setParameter("qname", qname.toString());
		q.setParameter("type", contentType);
		q.setParameter("version", version);

		Long count = (Long) q.getSingleResult();
		if (count.intValue() > 0) {
			throw new RepositoryException(String.format("Artifact qname %s contentType %s version %s already exists", qname, contentType, version));
		}
		ArtifactImpl artifact = new ArtifactImpl();
		artifact.setQName(qname);
		artifact.setVersion(version);
		artifact.setContentType(contentType);
		DataHandler dh = getDataHandler(content, contentType);
		try {
			artifact.setContent(dh.toContent());
		} catch (IOException e) {
			throw new RepositoryException(e);
		}
		mgr.getTransaction().begin();
		mgr.persist(artifact);
		mgr.getTransaction().commit();
	}

	/*
	 * void store(QName qname, String version, String type, DataHandler
	 * content){ String mime_type = null; //ObjectType <=> mimtype
	 * mappingqo.getType(); DataHandler my_dh = new DataHandler(content,
	 * mime_type); //my_dh.getInputStream() }
	 */

	@Override
	public Observable watch(QName qname, String version, String type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends CommandObject> void registerCommandInfo(String mimeType, String commandName, boolean preferred, Provider<T> provider) {
		commandMap.registerCommandInfo(mimeType, new CommandInfo<T>(commandName, null, true, provider));

	}

	@Override
	public void registerExtension(String fileExtension, String mimeType) {
		fileTypes.registerExtension(fileExtension, mimeType);
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
