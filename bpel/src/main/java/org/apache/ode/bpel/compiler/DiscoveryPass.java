/**
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
package org.apache.ode.bpel.compiler;

import java.io.ByteArrayInputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.apache.ode.bpel.compiler.parser.ExecutableProcessParser;
import org.apache.ode.bpel.exec.xml.Process;
import org.apache.ode.bpel.spi.BPELContext;
import org.apache.ode.spi.compiler.CompilerPass;
import org.apache.ode.spi.compiler.Contextual;
import org.apache.ode.spi.compiler.ExecCompilerContext;
import org.apache.ode.spi.compiler.Location;
public class DiscoveryPass implements CompilerPass<ExecCompilerContext> {

	@Override
	public void compile(ExecCompilerContext ctx) {
		BPELContext bctx = ctx.subContext(BPELContext.ID, BPELContext.class);
		bctx.getClass();
		switch (ctx.phase()) {
		case DISCOVERY:
			try {
				XMLInputFactory inputFactory = XMLInputFactory.newInstance();
				XMLStreamReader reader = inputFactory.createXMLStreamReader(new ByteArrayInputStream(ctx.source().getContent()));
				while (reader.hasNext()) {
					int type = reader.next();
					switch (type) {
					case XMLStreamConstants.START_ELEMENT:
						if (ExecutableProcessParser.EXECUTABLE.equals(reader.getName())) {
							Contextual<Process> model = new Contextual<Process>(ExecutableProcessParser.EXECUTABLE, Process.class,null);
							bctx.setMainModel(model);
							ctx.parseContent(reader, model);
						} else {
							ctx.addError(new Location(reader.getLocation()), String.format("Unsupported root element %s", reader.getName()), null);
							ctx.terminate();
							return;
						}
						break;
					}
				}
			} catch (Exception e) {
				ctx.addError(null,"Unable to parse BPEL document",e);
				ctx.terminate();
			}
			break;
		}
	}

}
