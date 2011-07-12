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

import java.util.logging.Logger;

import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;

import org.apache.ode.runtime.exec.wsdl.xml.Configuration;
import org.apache.ode.runtime.exec.wsdl.xml.ObjectFactory;
import org.apache.ode.spi.compiler.CompilerContext;
import org.apache.ode.spi.compiler.CompilerPass;
import org.apache.ode.spi.compiler.CompilerPhase;
import org.apache.ode.spi.compiler.Source;
import org.apache.ode.spi.compiler.Source.SourceType;
import org.apache.ode.spi.compiler.WSDLContext;
import org.apache.ode.spi.compiler.XSDContext;
import org.apache.ode.spi.exec.xml.Executable;
import org.apache.ode.spi.exec.xml.Installation;
import org.apache.ode.spi.exec.xml.InstructionSets;

public class WSDLCompiler implements CompilerPass {
	WSDLContext wsdlCtx;
	XSDContext xsdContext;
	QName wsdlQName;
	Definition wsdlDefinition;

	private static final Logger log = Logger.getLogger(WSDLCompiler.class.getName());

	@Override
	public void compile(CompilerContext ctx) {
		if (ctx.source().sourceType() == SourceType.MAIN) {
			ctx.addError(null,"WSDL is not a supported target executable", null);
			ctx.terminate();
			return;
		}

		switch (ctx.phase()) {
		case INITIALIZE:
			wsdlCtx = (WSDLContext) ctx.subContext(WSDLContext.ID);
			xsdContext = (XSDContext) ctx.subContext(XSDContext.ID);
			wsdlQName = wsdlCtx.declareWSDL(ctx.source());
			wsdlCtx.getExtensionRegistry();
			break;

		case DISCOVERY:
			WSDLReader reader = wsdlCtx.createWSDLReader();
			try {
				wsdlDefinition = reader.readWSDL(wsdlCtx.getWSDLLocator(wsdlQName, xsdContext));
				//log.info("got definition "+wsdlDefinition);
			} catch (WSDLException we) {
				ctx.addError(null,String.format("failed to read WSDL %s", ctx.source()), we);
			}
			// QName schemaLocation = (QName) definition.getExtensionAttribute(SCHEMA_LOCATION);
			break;

		case EMIT:
			ObjectFactory wsdlFactory = new ObjectFactory();
			Executable exec = ctx.executable();
			InstructionSets is = exec.getInstructionSets();
			if (is == null) {
				is = new InstructionSets();
				exec.setInstructionSets(is);
			}
			if (!is.getInstructionSet().contains(WSDLComponent.WSDL_INSTRUCTION_SET)) {
				is.getInstructionSet().add(WSDLComponent.WSDL_INSTRUCTION_SET);
			}
			Configuration config = new Configuration();
			config.setBase("HelloWorld");
			Installation install = exec.getInstallation();
			if (install == null) {
				install = new Installation();
				exec.setInstallation(install);
			}
			install.getAny().add(wsdlFactory.createConfiguration(config));
			break;
		}
	}

}
