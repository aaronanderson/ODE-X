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
package org.apache.ode.spi.repo;

import java.util.Observable;

import javax.activation.CommandObject;
import javax.inject.Provider;
import javax.xml.namespace.QName;

public interface Repository {

	<T extends CommandObject> void registerCommandInfo(String mimeType, String commandName, boolean preferred, Provider<T> provider);

	void registerExtension(String fileExtension, String mimeType);

	void registerHandler(String mimeType, DataContentHandler handler);

	DataHandler getDataHandler(ArtifactDataSource ds);

	<C> DataHandler getDataHandler(C content, String mimeType);

	// byte[] load(QName qname, String version, String type);
	<C> C load(QName qname, String version, String type, Class<C> javaType) throws RepositoryException;

	// XMLStreamReader loadXmlStream(QName qname, String version, String type);

	// Document loadXmlDom(QName qname, String version, String type);

	// void store(QName qname, String version, String type, byte[] content);
	<C> void store(QName qname, String version, String type, C content) throws RepositoryException;

	// void store(QName qname, String version, String type, XMLStreamReader
	// content);

	// void store(QName qname, String version, String type, Document content);

	Observable watch(QName qname, String version, String type);

}
