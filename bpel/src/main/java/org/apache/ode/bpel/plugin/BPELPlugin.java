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
package org.apache.ode.bpel.plugin;

import java.io.InputStream;

import javax.activation.DataSource;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.ode.bpel.repo.BPELExecValidation;
import org.apache.ode.spi.Plugin;
import org.apache.ode.spi.repo.Repository;
import org.apache.ode.spi.repo.XMLDataContentHandler;

@Singleton
@Named("BPELPlugin")
public class BPELPlugin implements Plugin {

	public static final String BPEL_EXEC_MIMETYPE = "application/bpel-exec";
	public static final String BPEL_EXEC_NAMESPACE = "http://docs.oasis-open.org/wsbpel/2.0/process/executable";
	// @Inject WSDLPlugin wsdlPlugin;
	@Inject
	Repository repository;
	@Inject
	Provider<BPELExecValidation> validateProvider;

	@PostConstruct
	public void init() {
		System.out.println("Initializing BPELPlugin");
		repository.registerFileExtension("bpel", BPEL_EXEC_MIMETYPE);
		repository.registerNamespace(BPEL_EXEC_NAMESPACE, BPEL_EXEC_MIMETYPE);
		repository.registerCommandInfo(BPEL_EXEC_MIMETYPE, "validate", true, validateProvider);
		repository.registerHandler(BPEL_EXEC_MIMETYPE, new XMLDataContentHandler() {

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
					if (name != null && tns != null) {
						defaultName = new QName(tns, name);
					}
					return defaultName;
				} catch (Exception e) {
					return null;
				}
			}
		});
		System.out.println("BPELPlugin Initialized");

	}

}
