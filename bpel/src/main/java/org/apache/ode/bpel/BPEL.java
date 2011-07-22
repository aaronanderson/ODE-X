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
package org.apache.ode.bpel;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.DataSource;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.ode.bpel.compiler.DiscoveryPass;
import org.apache.ode.bpel.compiler.EmitPass;
import org.apache.ode.bpel.compiler.parser.ExecutableProcessParser;
import org.apache.ode.bpel.compiler.parser.PartnerLinksParser;
import org.apache.ode.bpel.compiler.parser.SequenceParser;
import org.apache.ode.bpel.compiler.parser.VariablesParser;
import org.apache.ode.bpel.exec.BPELComponent;
import org.apache.ode.bpel.spi.BPELContext;
import org.apache.ode.spi.compiler.Compiler;
import org.apache.ode.spi.compiler.CompilerPhase;
import org.apache.ode.spi.compiler.Compilers;
import org.apache.ode.spi.compiler.wsdl.WSDLContext;
import org.apache.ode.spi.compiler.xsd.XSDContext;
import org.apache.ode.spi.exec.Platform;
import org.apache.ode.spi.exec.WSDLComponent;
import org.apache.ode.spi.repo.Repository;
import org.apache.ode.spi.repo.Validate;
import org.apache.ode.spi.repo.XMLDataContentHandler;
import org.apache.ode.spi.repo.XMLValidate;
import org.apache.ode.spi.repo.XMLValidate.SchemaSource;

@Singleton
@Named("BPELPlugin")
public class BPEL {

	public static final String BPEL_EXEC_MIMETYPE = "application/bpel-exec";
	public static final String BPEL_EXEC_NAMESPACE = "http://docs.oasis-open.org/wsbpel/2.0/process/executable";
	public static final String WSDL_MIMETYPE = "application/wsdl";
	private static final Logger log = Logger.getLogger(BPEL.class.getName());

	// @Inject WSDLPlugin wsdlPlugin;
	@Inject
	Repository repository;
	@Inject
	Compilers compilers;
	@Inject
	Platform platform;
	@Inject
	XMLValidate xmlValidate;
	@Inject
	BPELComponent bpelComponent;
	@Inject
	Provider<BPELContext> ctxProvider;
	@Inject
	Provider<XSDContext> schemaProvider;
	@Inject
	Provider<WSDLContext> wsdlProvider;

	@PostConstruct
	public void init() {
		log.fine("Initializing BPELPlugin");
		repository.registerFileExtension("bpel", BPEL_EXEC_MIMETYPE);
		repository.registerNamespace(BPEL_EXEC_NAMESPACE, BPEL_EXEC_MIMETYPE);
		xmlValidate.registerSchemaSource(BPEL_EXEC_MIMETYPE, new SchemaSource() {

			@Override
			public Source[] getSchemaSource() {
				try {
					return new Source[] { new StreamSource(getClass().getResourceAsStream("/META-INF/xsd/ws-bpel_executable.xsd")) };
				} catch (Exception e) {
					log.log(Level.SEVERE, "", e);
					return null;
				}
			}
		});

		xmlValidate.registerSchemaSource(WSDL_MIMETYPE, new SchemaSource() {

			@Override
			public Source[] getSchemaSource() {
				try {
					return new Source[] { new StreamSource(getClass().getResourceAsStream("/META-INF/xsd/ws-bpel_plnktype.xsd")) };
				} catch (Exception e) {
					log.log(Level.SEVERE, "", e);
					return null;
				}
			}
		});
		repository.registerCommandInfo(BPEL_EXEC_MIMETYPE, Validate.VALIDATE_CMD, true, xmlValidate.getProvider());
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
		platform.registerComponent(bpelComponent);
		Compiler bpelCompiler = compilers.newInstance();
		bpelCompiler.addInstructionSet(BPELComponent.BPEL_INSTRUCTION_SET);
		bpelCompiler.addInstructionSet(WSDLComponent.WSDL_INSTRUCTION_SET);
		bpelCompiler.addSubContext(XSDContext.ID, schemaProvider);
		bpelCompiler.addSubContext(WSDLContext.ID, wsdlProvider);
		bpelCompiler.addSubContext(BPELContext.ID, ctxProvider);
		bpelCompiler.addCompilerPass(CompilerPhase.INITIALIZE, new DiscoveryPass());
		bpelCompiler.addCompilerPass(CompilerPhase.DISCOVERY, new DiscoveryPass());
		bpelCompiler.addCompilerPass(CompilerPhase.EMIT, new EmitPass());
		// bpelCompiler.addCompilerPass(CompilerPhase.LINK, new
		// DiscoveryPass());
		// bpelCompiler.addCompilerPass(CompilerPhase.VALIDATE, new
		// DiscoveryPass());
		// bpelCompiler.addCompilerPass(CompilerPhase.EMIT, new
		// DiscoveryPass());
		// bpelCompiler.addCompilerPass(CompilerPhase.ANALYSIS, new
		// DiscoveryPass());
		// bpelCompiler.addCompilerPass(CompilerPhase.FINALIZE, new
		// DiscoveryPass());
		compilers.register(bpelCompiler, BPEL_EXEC_MIMETYPE);
		bpelCompiler.addContentParser(new ExecutableProcessParser(), ExecutableProcessParser.EXECUTABLE);
		bpelCompiler.addContentParser(new PartnerLinksParser(), PartnerLinksParser.PARTNERLINKS);
		bpelCompiler.addContentParser(new VariablesParser(), VariablesParser.VARIABLES);
		bpelCompiler.addContentParser(new SequenceParser(), SequenceParser.SEQUENCE);
		log.fine("BPELPlugin Initialized");

	}

}
