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
package org.apache.ode.spi.repo;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import javax.activation.CommandObject;
import javax.inject.Provider;

public interface Repository {

	<T extends CommandObject> void registerCommandInfo(String mimeType, String commandName, boolean preferred, Provider<T> provider);

	void registerFileExtension(String fileExtension, String mimeType);

	void registerNamespace(String namespace, String mimeType);

	void registerHandler(String mimeType, DataContentHandler handler);

	DataHandler getDataHandler(ArtifactDataSource ds);

	<C> DataHandler getDataHandler(C content, String mimeType);

	<C> UUID create(URI uri, String version, String type, C content) throws RepositoryException;

	<R,C> C read(R criteria, Class<C> javaType) throws RepositoryException;
	
	<R,C> List<C> search(R criteria, Class<C> javaType, long limit) throws RepositoryException;

	<R,C> void update(R criteria, C content) throws RepositoryException;

	<R> void delete(R criteria) throws RepositoryException;

	<R> boolean exists(R criteria) throws RepositoryException;

	// void store(URI uri, String version, String type, XMLStreamReader

}
