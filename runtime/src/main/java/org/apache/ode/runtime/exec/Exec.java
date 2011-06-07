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
package org.apache.ode.runtime.exec;

import java.io.InputStream;

import javax.activation.DataSource;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.ode.runtime.exec.platform.PlatformImpl;
import org.apache.ode.spi.exec.Platform;
import org.apache.ode.spi.repo.JAXBDataContentHandler;
import org.apache.ode.spi.repo.Repository;

import static org.apache.ode.runtime.exec.platform.Cluster.*;

@Singleton
public class Exec {

	@Inject
	Repository repository;
	@Inject
	PlatformImpl platform;

	@PostConstruct
	public void init() {
		System.out.println("Initializing Execution Runtime");
		repository.registerFileExtension("exec", Platform.EXEC_MIMETYPE);
		repository.registerNamespace(Platform.EXEC_NAMESPACE, Platform.EXEC_MIMETYPE);
		repository.registerHandler(Platform.EXEC_MIMETYPE, new ExecDataContentHandler(platform));

		repository.registerNamespace(CLUSTER_CONFIG_NAMESPACE, CLUSTER_CONFIG_MIMETYPE);
		repository.registerHandler(CLUSTER_CONFIG_MIMETYPE, new JAXBDataContentHandler(CLUSTER_CONFIG_JAXB_CTX) {
			@Override
			public QName getDefaultQName(DataSource dataSource) {
				QName defaultName = null;
				try {
					InputStream is = dataSource.getInputStream();
					XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(is);
					reader.nextTag();
					String tns = CLUSTER_CONFIG_NAMESPACE;
					String name = reader.getAttributeValue(null, "name");
					reader.close();
					if (name != null) {
						defaultName = new QName(tns, name);
					}
					return defaultName;
				} catch (Exception e) {
					return null;
				}
			}

		});
		System.out.println("Execution Runtime Initialized");

	}
}
