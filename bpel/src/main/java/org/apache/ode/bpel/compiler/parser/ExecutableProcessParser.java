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
import org.apache.ode.spi.compiler.Parser;
import org.apache.ode.spi.compiler.ParserException;
import org.apache.ode.spi.compiler.ParserUtils;

public class ExecutableProcessParser implements Parser<Contextual<Process>> {
	public static final QName EXECUTABLE = new QName(BPEL.BPEL_EXEC_NAMESPACE, "process");
	public static final QName IMPORT = new QName(BPEL.BPEL_EXEC_NAMESPACE, "import");
	public static final QName IMPORT_SETTING = new QName(BPEL.BPEL_EXEC_NAMESPACE, "import");

	@Override
	public void parse(XMLStreamReader input, Contextual<Process> model, CompilerContext context) throws XMLStreamException, ParserException {
		while (input.hasNext()) {
			int type = input.getEventType();
			switch (type) {
			case XMLStreamConstants.START_ELEMENT:
				ParserUtils.assertStart(input, EXECUTABLE);
				ParserUtils.setLocation(input, context.source().srcRef(), model);
				model.beginContext().setQueryLanguage(input.getAttributeValue(BPEL.BPEL_EXEC_NAMESPACE, "queryLanguage"));
				model.beginContext().setExpressionLanguage(input.getAttributeValue(BPEL.BPEL_EXEC_NAMESPACE, "expressionLanguage"));

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

	public void parseImport(XMLStreamReader input, Contextual<Process> model, CompilerContext context) throws XMLStreamException, ParserException {
		while (input.hasNext()) {
			int type = input.getEventType();
			switch (type) {
			case XMLStreamConstants.START_ELEMENT:
				ParserUtils.assertStart(input, IMPORT);
				Import imprt = new Import();
				imprt.setType(input.getAttributeValue(IMPORT.getNamespaceURI(), "importType"));
				imprt.setNamespace(input.getAttributeValue(IMPORT.getNamespaceURI(), "namespace"));
				imprt.setLocation(input.getAttributeValue(IMPORT.getNamespaceURI(), "location"));
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
