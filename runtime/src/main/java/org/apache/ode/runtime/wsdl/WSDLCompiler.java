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

import org.apache.ode.runtime.exec.wsdl.xml.Configuration;
import org.apache.ode.runtime.exec.wsdl.xml.ObjectFactory;
import org.apache.ode.spi.compiler.CompilerContext;
import org.apache.ode.spi.compiler.CompilerPass;
import org.apache.ode.spi.compiler.CompilerPhase;
import org.apache.ode.spi.compiler.Source;
import org.apache.ode.spi.compiler.Source.SourceType;
import org.apache.ode.spi.exec.xml.Executable;
import org.apache.ode.spi.exec.xml.Installation;
import org.apache.ode.spi.exec.xml.InstructionSets;

public class WSDLCompiler implements CompilerPass {

	@Override
	public void compile(CompilerPhase phase, CompilerContext ctx, Source artifact) {
		if (artifact.sourceType() == SourceType.MAIN) {
			ctx.addError("WSDL is not a supported target executable", null);
			ctx.terminate();
			return;
		}
		switch (phase) {
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
