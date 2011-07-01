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
package org.apache.ode.runtime.xsl;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.ode.spi.repo.Repository;

@Singleton
public class XSL {

	public static final String XSL_MIMETYPE = "application/wsl";
	public static final String XSL_NAMESPACE = "http://www.w3.org/1999/XSL/Transform";
	// @Inject WSDLPlugin wsdlPlugin;
	@Inject
	Repository repository;
	@Inject
	Provider<XSLValidation> validateProvider;
	@Inject
	Provider<XSLTransform> transformProvider;

	@PostConstruct
	public void init() {
		System.out.println("Initializing XSL support");
		repository.registerFileExtension("xsl", XSL_MIMETYPE);
		repository.registerFileExtension("xslt", XSL_MIMETYPE);
		repository.registerNamespace(XSL_NAMESPACE, XSL_MIMETYPE);
		repository.registerCommandInfo(XSL_MIMETYPE, "validate", true, validateProvider);
		repository.registerCommandInfo(XSL_MIMETYPE, "transform", true, transformProvider);
		repository.registerHandler(XSL_MIMETYPE, new XSLDataContentHandler());
		System.out.println("XSL support Initialized");
	}

}