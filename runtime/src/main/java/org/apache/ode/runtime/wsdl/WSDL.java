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
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.ode.spi.compiler.Compiler;
import org.apache.ode.spi.compiler.CompilerPhase;
import org.apache.ode.spi.compiler.Compilers;
import org.apache.ode.spi.compiler.WSDLContext;
import org.apache.ode.spi.compiler.XMLSchemaContext;
import org.apache.ode.spi.exec.Platform;
import org.apache.ode.spi.repo.Repository;
import org.apache.ode.spi.repo.Validate;
import org.apache.ode.spi.repo.XMLValidate;
import org.apache.ode.spi.repo.XMLValidate.SchemaSource;

@Singleton
public class WSDL {

	public static final String WSDL_MIMETYPE= "application/wsdl";
	public static final String WSDL_NAMESPACE ="http://schemas.xmlsoap.org/wsdl/";

	//@Inject WSDLPlugin wsdlPlugin;
	@Inject
	Repository repository;
	@Inject 
	XMLValidate xmlValidate;
	@Inject
	Compilers compilers;
	@Inject
	Platform platform;
	@Inject
	WSDLComponent wsdlComponent;
	@Inject
	Provider<XMLSchemaContext> schemaProvider;
	@Inject
	Provider<WSDLContext> wsdlProvider;
	
	
	@PostConstruct
	public void init(){
		System.out.println("Initializing WSDL support");
		repository.registerFileExtension("wsdl", WSDL_MIMETYPE);
		repository.registerNamespace(WSDL_NAMESPACE, WSDL_MIMETYPE);
		xmlValidate.registerSchemaSource(WSDL_MIMETYPE, new SchemaSource() {
			
			@Override
			public Source[] getSchemaSource() {
				try {
					return new Source[] { new StreamSource(getClass().getResourceAsStream("/META-INF/xsd/wsdl.xsd"))};
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}
		});
		repository.registerCommandInfo(WSDL_MIMETYPE, Validate.VALIDATE_CMD, true, xmlValidate.getProvider());
		repository.registerHandler(WSDL_MIMETYPE, new WSDLDataContentHandler());
		platform.registerComponent(wsdlComponent);
		Compiler wsdlCompiler = compilers.newInstance();
		wsdlCompiler.addInstructionSet(wsdlComponent.instructionSets().get(0).getName());
		wsdlCompiler.addSubContext(XMLSchemaContext.ID, schemaProvider);
		wsdlCompiler.addSubContext(WSDLContext.ID,wsdlProvider);
		WSDLCompiler compiler = new WSDLCompiler();
		wsdlCompiler.addCompilerPass(CompilerPhase.DISCOVERY, compiler);
		wsdlCompiler.addCompilerPass(CompilerPhase.EMIT, compiler);
		//bpelCompiler.addCompilerPass(CompilerPhase.LINK, new DiscoveryPass());
		//bpelCompiler.addCompilerPass(CompilerPhase.VALIDATE, new DiscoveryPass());
		//bpelCompiler.addCompilerPass(CompilerPhase.EMIT, new DiscoveryPass());
		//bpelCompiler.addCompilerPass(CompilerPhase.ANALYSIS, new DiscoveryPass());
		//bpelCompiler.addCompilerPass(CompilerPhase.FINALIZE, new DiscoveryPass());
		compilers.register(wsdlCompiler, WSDL_MIMETYPE);
		System.out.println("WSDL support Initialized");
		
	}
	
}
