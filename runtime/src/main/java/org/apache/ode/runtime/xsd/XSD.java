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

import org.apache.ode.spi.compiler.Compiler;
import org.apache.ode.spi.compiler.CompilerPhase;
import org.apache.ode.spi.compiler.Compilers;
import org.apache.ode.spi.compiler.XMLSchemaContext;
import org.apache.ode.spi.repo.Repository;
import org.apache.ode.spi.repo.XMLDataContentHandler;

@Singleton
public class XSD {

	public static final String XSD_MIMETYPE = "application/xsd";
	public static final String XSD_NAMESPACE = "http://www.w3.org/2001/XMLSchema";

	@Inject
	Repository repository;
	@Inject
	Compilers compilers;
	@Inject
	Provider<XMLSchemaContext> schemaProvider;
	
	@PostConstruct
	public void init() {
		System.out.println("Initializing XSD support");
		repository.registerFileExtension("xsd", XSD_MIMETYPE);
		repository.registerNamespace(XSD_NAMESPACE, XSD_MIMETYPE);
		repository.registerHandler(XSD_MIMETYPE, new XMLDataContentHandler());
		
		Compiler schemaCompiler = compilers.newInstance();
		schemaCompiler.addSubContext(schemaProvider);
		XSDCompiler compiler = new XSDCompiler();
		schemaCompiler.addCompilerPass(CompilerPhase.DISCOVERY, compiler);
		//bpelCompiler.addCompilerPass(CompilerPhase.LINK, new DiscoveryPass());
		//bpelCompiler.addCompilerPass(CompilerPhase.VALIDATE, new DiscoveryPass());
		//bpelCompiler.addCompilerPass(CompilerPhase.EMIT, new DiscoveryPass());
		//bpelCompiler.addCompilerPass(CompilerPhase.ANALYSIS, new DiscoveryPass());
		//bpelCompiler.addCompilerPass(CompilerPhase.FINALIZE, new DiscoveryPass());
		compilers.register(schemaCompiler, XSD_MIMETYPE);
		
		System.out.println("XSD support Initialized");
	}

}
