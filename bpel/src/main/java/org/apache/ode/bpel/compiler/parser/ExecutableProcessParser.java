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
package org.apache.ode.bpel.compiler.parser;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.ode.bpel.BPEL;
import org.apache.ode.bpel.exec.xml.Import;
import org.apache.ode.bpel.exec.xml.Process;
import org.apache.ode.spi.compiler.CompilerContext;
import org.apache.ode.spi.compiler.Contextual;
import org.apache.ode.spi.compiler.ElementParser;
import org.apache.ode.spi.compiler.ExecCompilerContext;
import org.apache.ode.spi.compiler.ParserException;
import org.apache.ode.spi.compiler.ParserUtils;

public class ExecutableProcessParser implements ElementParser<Contextual<Process>> {
	public static final QName EXECUTABLE = new QName(BPEL.BPEL_EXEC_NAMESPACE, "process");
	public static final QName IMPORT = new QName(BPEL.BPEL_EXEC_NAMESPACE, "import");
	public static final QName IMPORT_SETTING = new QName(BPEL.BPEL_EXEC_NAMESPACE, "import");

	@Override
	public void parse(XMLStreamReader input, Contextual<Process> model, ExecCompilerContext context) throws XMLStreamException, ParserException {
		while (input.hasNext()) {
			int type = input.getEventType();
			switch (type) {
			case XMLStreamConstants.START_ELEMENT:
				ParserUtils.assertStart(input, EXECUTABLE);
				ParserUtils.setLocation(input, context.source().srcRef(), model);
				String[] attrs = context.parseAttributes(input, model, "name", "targetNamespace", "queryLanguage", "expressionLanguage", "suppressJoinFailure",
						"exitOnStandardFault");

				model.beginContext().setName(attrs[0]);
				model.beginContext().setTargetNamespace(attrs[1]);
				model.beginContext().setQueryLanguage(attrs[2]);
				model.beginContext().setExpressionLanguage(attrs[3]);
				model.beginContext().setSuppressJoinFailure("yes".equals(attrs[4]) ? true : false);
				model.beginContext().setExitOnStandardFault("yes".equals(attrs[5]) ? true : false);

				// ParserUtils.skipChildren(input);

				while (input.nextTag() == XMLStreamConstants.START_ELEMENT) {
					if (IMPORT.equals(input.getName())) {
						parseImport(input, model, context);
					} else {
						context.parseContent(input, model);
					}
				}
				break;
			case XMLStreamConstants.END_ELEMENT:
				ParserUtils.assertEnd(input, EXECUTABLE);
				return;
			}
		}

	}

	public void parseImport(XMLStreamReader input, Contextual<Process> model, ExecCompilerContext context) throws XMLStreamException, ParserException {
		while (input.hasNext()) {
			int type = input.getEventType();
			switch (type) {
			case XMLStreamConstants.START_ELEMENT:
				ParserUtils.assertStart(input, IMPORT);
				Import imprt = new Import();
				String[] attrs = context.parseAttributes(input, model, "importType", "namespace", "location");
				imprt.setType(attrs[0]);
				imprt.setNamespace(attrs[1]);
				imprt.setLocation(attrs[2]);
				// model.settings().put(, value);
				while (input.nextTag() == XMLStreamConstants.START_ELEMENT) {
					context.parseContent(input, model);
				}
				break;
			case XMLStreamConstants.END_ELEMENT:
				ParserUtils.assertEnd(input, IMPORT);
				return;
			}
		}
	}

}
