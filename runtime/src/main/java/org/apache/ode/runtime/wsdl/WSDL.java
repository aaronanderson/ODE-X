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
package org.apache.ode.runtime.wsdl;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.ode.spi.repo.Repository;

@Singleton
public class WSDL {

	public static final String WSDL_MIMETYPE= "application/wsdl";
	public static final String WSDL_NAMESPACE ="http://schemas.xmlsoap.org/wsdl/";
	//@Inject WSDLPlugin wsdlPlugin;
	@Inject
	Repository repository;
	@Inject 
	Provider<WSDLValidation> validateProvider;
	
	@PostConstruct
	public void init(){
		System.out.println("Initializing WSDL support");
		repository.registerFileExtension("wsdl", WSDL_MIMETYPE);
		repository.registerNamespace(WSDL_NAMESPACE, WSDL_MIMETYPE);
		repository.registerCommandInfo(WSDL_MIMETYPE, "validate", true, validateProvider);
		repository.registerHandler(WSDL_MIMETYPE, new WSDLDataContentHandler());
		System.out.println("WSDL support Initialized");
		
	}
	
}
