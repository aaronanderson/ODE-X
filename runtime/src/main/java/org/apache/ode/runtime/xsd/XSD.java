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
package org.apache.ode.runtime.xsd;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.ode.spi.repo.Repository;
import org.apache.ode.spi.repo.XMLDataContentHandler;

@Singleton
public class XSD {

	public static final String XSD_MIMETYPE = "application/xsd";
	public static final String XSD_NAMESPACE = "http://www.w3.org/2001/XMLSchema";

	@Inject
	Repository repository;
	
	@PostConstruct
	public void init() {
		System.out.println("Initializing XSD support");
		repository.registerFileExtension("xsd", XSD_MIMETYPE);
		repository.registerNamespace(XSD_NAMESPACE, XSD_MIMETYPE);
		repository.registerHandler(XSD_MIMETYPE, new XMLDataContentHandler());
		System.out.println("XSD support Initialized");
	}

}
