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
package org.apache.ode.wsht;

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

import org.apache.ode.spi.compiler.CompilerPhase;
import org.apache.ode.spi.compiler.Compilers;
import org.apache.ode.spi.compiler.ExecCompiler;
import org.apache.ode.spi.compiler.ParserException;
import org.apache.ode.spi.compiler.ParserRegistry;
import org.apache.ode.spi.compiler.wsdl.WSDLContext;
import org.apache.ode.spi.compiler.xsd.XSDContext;
import org.apache.ode.spi.exec.Platform;
import org.apache.ode.spi.exec.WSDLComponent;
import org.apache.ode.spi.repo.Repository;
import org.apache.ode.spi.repo.Validate;
import org.apache.ode.spi.repo.XMLDataContentHandler;
import org.apache.ode.spi.repo.XMLValidate;
import org.apache.ode.spi.repo.XMLValidate.SchemaSource;
import org.apache.ode.wsht.compiler.DiscoveryPass;
import org.apache.ode.wsht.compiler.EmitPass;
import org.apache.ode.wsht.compiler.parser.HumanInteractionsParser;
import org.apache.ode.wsht.exec.WSHTComponent;
import org.apache.ode.wsht.spi.WSHTContext;

@Singleton
@Named("WSHTPlugin")
public class WSHT {

	public static final String WSHT_MIMETYPE = "application/wsht";
	public static final String WSHT_NAMESPACE = "http://docs.oasis-open.org/ns/bpel4people/ws-humantask/200803";
	public static final String WSDL_MIMETYPE = "application/wsdl";
	private static final Logger log = Logger.getLogger(WSHT.class.getName());

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
	WSHTComponent wshtComponent;
	@Inject
	Provider<WSHTContext> ctxProvider;
	@Inject
	Provider<XSDContext> schemaProvider;
	@Inject
	Provider<WSDLContext> wsdlProvider;

	@PostConstruct
	public void init() {
		log.fine("Initializing WSHTPlugin");
		repository.registerFileExtension("hi", WSHT_MIMETYPE);
		repository.registerNamespace(WSHT_NAMESPACE, WSHT_MIMETYPE);
		xmlValidate.registerSchemaSource(WSHT_MIMETYPE, new SchemaSource() {

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
		repository.registerCommandInfo(WSHT_MIMETYPE, Validate.VALIDATE_CMD, true, xmlValidate.getProvider());
		repository.registerHandler(WSHT_MIMETYPE, new XMLDataContentHandler() {

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
		platform.registerComponent(wshtComponent);
		// (ExecCompiler) new XMLCompilerImpl<Unit<? extends Instruction>, ExecCompilerContext, ElementParser<? extends Unit<? extends Instruction>>,
		// AttributeParser<? extends Unit<? extends Instruction>>>
		ExecCompiler wshtCompiler = new ExecCompiler();
		wshtCompiler.addInstructionSet(WSHTComponent.WSHT_INSTRUCTION_SET);
		wshtCompiler.addInstructionSet(WSDLComponent.WSDL_INSTRUCTION_SET);
		wshtCompiler.addSubContext(XSDContext.ID, schemaProvider);
		wshtCompiler.addSubContext(WSDLContext.ID, wsdlProvider);
		wshtCompiler.addSubContext(WSHTContext.ID, ctxProvider);
		wshtCompiler.addCompilerPass(CompilerPhase.INITIALIZE, new DiscoveryPass());
		wshtCompiler.addCompilerPass(CompilerPhase.DISCOVERY, new DiscoveryPass());
		wshtCompiler.addCompilerPass(CompilerPhase.EMIT, new EmitPass());
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
		ParserRegistry preg = wshtCompiler.handlerRegistry(ParserRegistry.class);
		try {
			preg.register(new HumanInteractionsParser(), HumanInteractionsParser.HUMAN_INTERACTIONS);
			
		} catch (ParserException pe) {
			log.log(Level.SEVERE, "", pe);
		}
		compilers.register(wshtCompiler, WSHT_MIMETYPE);

	
		log.fine("WSHTPlugin Initialized");

	}

}
