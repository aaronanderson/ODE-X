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
package org.apache.ode.runtime.wsdl.compiler.parser;

import static org.apache.ode.spi.compiler.wsdl.Definition.DEFINITIONS;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.ode.spi.compiler.ParserException;
import org.apache.ode.spi.compiler.ParserUtils;
import org.apache.ode.spi.compiler.wsdl.Definition;
import org.apache.ode.spi.compiler.wsdl.ElementParser;
import org.apache.ode.spi.compiler.wsdl.Unit;
import org.apache.ode.spi.compiler.wsdl.WSDLCompilerContext;

public class DefinitionParser implements ElementParser<Definition> {

	final public static QName IMPORT = new QName(Unit.WSDL_NS, "import");
	final public static QName TYPES = new QName(Unit.WSDL_NS, "types");

	@Override
	public void parse(XMLStreamReader input, Definition def, WSDLCompilerContext context) throws XMLStreamException, ParserException {
		while (input.hasNext()) {
			int type = input.getEventType();
			switch (type) {
			case XMLStreamConstants.START_ELEMENT:
				ParserUtils.assertStart(input, DEFINITIONS);
				// ParserUtils.setLocation(input, context.source().srcRef(), assign);
				// model.children().add(assign);
				while (input.nextTag() == XMLStreamConstants.START_ELEMENT) {
					if (IMPORT.equals(input.getName())) {

					} else if (TYPES.equals(input.getName())) {

					} else {
						context.parseContent(input, def);
					}
				}
				break;
			case XMLStreamConstants.END_ELEMENT:
				ParserUtils.assertEnd(input, DEFINITIONS);
				return;
			}
		}

	}

	public void parseImport(XMLStreamReader input, Definition model, WSDLCompilerContext context) throws XMLStreamException, ParserException {
		while (input.hasNext()) {
			int type = input.getEventType();
			switch (type) {
			case XMLStreamConstants.START_ELEMENT:
				ParserUtils.assertStart(input, IMPORT);
				String[] attrs = context.parseAttributes(input, model, "namespace", "location");

				break;
			case XMLStreamConstants.END_ELEMENT:
				ParserUtils.assertEnd(input, IMPORT);
				return;
			}
		}
	}

}
