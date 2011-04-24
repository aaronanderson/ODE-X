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
package org.apache.ode.runtime.jmx;

import java.io.IOException;

import javax.activation.MimeTypeParseException;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.xml.namespace.QName;

import org.apache.ode.spi.repo.ArtifactDataSource;
import org.apache.ode.spi.repo.Repository;
import org.apache.ode.spi.repo.RepositoryException;

public class RepositoryImpl implements org.apache.ode.api.Repository {
	
	@Inject
	Repository repo;
	@Inject
	Provider<ArtifactDataSource> dsProvider;

	@Override
	public ArtifactId importFile(String name, String type, String version, String fileName, byte[] contents) throws IOException {
		if (contents == null || contents.length == 0) {
			throw new IOException("File contents is empty");
		}
		System.out.println("Import " + fileName);
		ArtifactDataSource ds = dsProvider.get();
		try {
			if (fileName!=null){
				ds.configure(contents, fileName);
			}else{
				ds.configure(contents);
			}
		} catch (MimeTypeParseException e) {
			throw new IOException(e);
		}
		String mtype = type != null ? type : ds.getContentType();
		QName qname = new QName(name);
		try {
			repo.store(qname, version, mtype, ds);
		} catch (RepositoryException e) {
			throw new IOException(e);
		}
		return new ArtifactId(qname.toString(), mtype, version);
	}

	@Override
	public byte[] exportFile(ArtifactId artifact) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type[] listTypes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArtifactId[] list(String type, int resultLimit) {
		// TODO Auto-generated method stub
		return null;
	}

}
