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

import static org.apache.ode.spi.compiler.wsdl.Message.MESSAGE;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.ode.spi.compiler.ParserException;
import org.apache.ode.spi.compiler.ParserUtils;
import org.apache.ode.spi.compiler.wsdl.Definition;
import org.apache.ode.spi.compiler.wsdl.ElementParser;
import org.apache.ode.spi.compiler.wsdl.Message;
import org.apache.ode.spi.compiler.wsdl.Message.Part;
import org.apache.ode.spi.compiler.wsdl.Unit;
import org.apache.ode.spi.compiler.wsdl.WSDLCompilerContext;

public class MessageParser implements ElementParser<Definition> {
	final public static QName PART = new QName(Unit.WSDL_NS, "part");

	@Override
	public void parse(XMLStreamReader input, Definition def, WSDLCompilerContext context) throws XMLStreamException, ParserException {
		ParserUtils.assertStart(input, MESSAGE);
		Message message = new Message();
		String[] attrs = context.parseAttributes(input, message, "name");

		// ParserUtils.setLocation(input, context.source().srcRef(), assign);
		// model.children().add(assign);
		// ParserUtils.skipChildren(input);
		while (input.nextTag() == XMLStreamConstants.START_ELEMENT) {
			if (PART.equals(input.getName())) {
				parsePart(input, message, context);
			} else {
				context.parseContent(input, message);
			}
		}
		ParserUtils.assertEnd(input, MESSAGE);

	}

	public void parsePart(XMLStreamReader input, Message msg, WSDLCompilerContext context) throws XMLStreamException, ParserException {
		ParserUtils.assertStart(input, PART);
		Part part = new Part();
		String[] pattrs = context.parseAttributes(input, part, "name", "element", "type");

		while (input.nextTag() == XMLStreamConstants.START_ELEMENT) {
			context.parseContent(input, part);
		}
		ParserUtils.assertEnd(input, PART);

	}

}