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
package org.apache.ode.runtime.build;

import java.io.InputStream;

import javax.activation.DataSource;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.ode.spi.repo.JAXBDataContentHandler;
import org.apache.ode.spi.repo.Repository;

@Singleton
public class BuildSystem {
	public static final String BUILDPLAN_MIMETYPE = "application/ode-build-plan";
	public static final String BUILDPLAN_NAMESPACE = "http://ode.apache.org/build-plan";
	@Inject
	Repository repository;
	@Inject
	Provider<BuildExecutor> buildProvider;

	@PostConstruct
	public void init() {
		System.out.println("Initializing BuildSystem");
		repository.registerFileExtension("build", BUILDPLAN_MIMETYPE);
		repository.registerFileExtension(BUILDPLAN_NAMESPACE, BUILDPLAN_MIMETYPE);
		repository.registerCommandInfo(BUILDPLAN_MIMETYPE, "build", true, buildProvider);
		try {
			JAXBContext jc = JAXBContext.newInstance("org.apache.ode.runtime.build.xml");
			repository.registerHandler(BUILDPLAN_MIMETYPE, new JAXBDataContentHandler(jc) {
				@Override
				public QName getDefaultQName(DataSource dataSource) {
					QName defaultName = null;
					try {
						InputStream is = dataSource.getInputStream();
						XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(is);
						reader.nextTag();
						String tns = reader.getAttributeValue(null, "targetNamespace");
						String name = reader.getAttributeValue(null, "name");
						reader.close();
						if (tns != null) {
							if (name != null) {
								defaultName = new QName(tns, name);
							} else {
								defaultName = QName.valueOf("{" + tns + "}");	
							}
						}
						return defaultName;
					} catch (Exception e) {
						return null;
					}
				}
			});
		} catch (JAXBException je) {
			je.printStackTrace();
		}

		System.out.println("BuildSystem Initialized");

	}

}
