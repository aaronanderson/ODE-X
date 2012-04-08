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
package org.apache.ode.runtime.jmx;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.CommandInfo;
import javax.activation.MimeTypeParseException;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.xml.namespace.QName;

import org.apache.ode.spi.repo.ArtifactDataSource;
import org.apache.ode.spi.repo.DataHandler;
import org.apache.ode.spi.repo.Repository;
import org.apache.ode.spi.repo.RepositoryException;
import org.apache.ode.spi.repo.Validate;

public class RepositoryImpl implements org.apache.ode.api.Repository {

	@Inject
	Repository repo;
	@Inject
	Provider<ArtifactDataSource> dsProvider;

	private static final Logger log = Logger.getLogger(RepositoryImpl.class.getName());

	@Override
	public ArtifactId importArtifact(ArtifactId artifactId, String fileName, boolean overwrite, boolean noValidate, byte[] contents) throws IOException {
		if (contents == null || contents.length == 0) {
			throw new IOException("File contents is empty");
		}
		log.log(Level.FINE, "Import {0}", fileName);
		ArtifactDataSource ds = dsProvider.get();
		try {
			if (fileName != null) {
				ds.configure(contents, fileName);
			} else {
				ds.configure(contents);
			}
		} catch (MimeTypeParseException e) {
			throw new IOException(e);
		}
		QName qname = null;
		if (artifactId.getName() == null) {
			DataHandler dh = repo.getDataHandler(ds);
			qname = dh.getDefaultQName();
			if (qname == null) {
				throw new IOException("Unable to establish default QName");
			}
		} else {
			qname = QName.valueOf(artifactId.getName());
		}
		String mtype = artifactId.getType() != null ? artifactId.getType() : ds.getContentType();
		String version = artifactId.getVersion() != null ? artifactId.getVersion() : "1.0";
		noValidate = true; // TODO validation is slow (30 sec), check to see why (background dl?)
		if (!noValidate) {
			DataHandler dh = repo.getDataHandler(ds);
			CommandInfo info = dh.getCommand(Validate.VALIDATE_CMD);
			if (info != null) {
				Validate cmd = (Validate) dh.getBean(info);
				StringBuilder sb = new StringBuilder();
				if (!cmd.validate(sb)) {
					throw new IOException(sb.toString());
				}
			}
		}

		try {
			if (overwrite && repo.exists(qname, mtype, version)) {
				repo.update(qname, mtype, version, ds);
			} else {
				repo.create(qname, mtype, version, ds);
			}
		} catch (RepositoryException e) {
			throw new IOException(e);
		}
		return new ArtifactId(qname.toString(), mtype, version);
	}

	@Override
	public void refreshArtifact(ArtifactId artifactId, boolean noValidate, byte[] contents) throws IOException {
		if (contents == null || contents.length == 0) {
			throw new IOException("File contents is empty");
		}
		log.log(Level.FINE, "Refresh {0}", artifactId);
		ArtifactDataSource ds = dsProvider.get();
		try {
			ds.configure(artifactId.getType(), contents);
		} catch (MimeTypeParseException e) {
			throw new IOException(e);
		}
		QName qname = QName.valueOf(artifactId.getName());
		try {
			repo.update(qname, artifactId.getType(), artifactId.getVersion(), ds);
		} catch (RepositoryException e) {
			throw new IOException(e);
		}
	}

	@Override
	public byte[] exportArtifact(ArtifactId artifactId) throws IOException {
		log.log(Level.FINE, "Export {0}", artifactId);
		QName qname = QName.valueOf(artifactId.getName());
		try {
			return repo.read(qname, artifactId.getType(), artifactId.getVersion(), byte[].class);
		} catch (RepositoryException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void removeArtifact(ArtifactId artifactId) throws IOException {
		log.log(Level.FINE, "Remove {0}", artifactId);
		QName qname = QName.valueOf(artifactId.getName());
		try {
			repo.delete(qname, artifactId.getType(), artifactId.getVersion());
		} catch (RepositoryException e) {
			throw new IOException(e);
		}
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
