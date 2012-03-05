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

import static org.apache.ode.bpel.compiler.parser.VariablesParser.FROM;
import static org.apache.ode.bpel.compiler.parser.VariablesParser.QUERY;
import static org.apache.ode.bpel.compiler.parser.VariablesParser.parseFrom;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.ode.bpel.BPEL;
import org.apache.ode.bpel.exec.xml.Assign;
import org.apache.ode.bpel.exec.xml.Copy;
import org.apache.ode.bpel.exec.xml.Scope;
import org.apache.ode.bpel.exec.xml.To;
import org.apache.ode.bpel.exec.xml.To.Query;
import org.apache.ode.bpel.exec.xml.To.Value;
import org.apache.ode.spi.compiler.Contextual;
import org.apache.ode.spi.compiler.ElementParser;
import org.apache.ode.spi.compiler.ExecCompilerContext;
import org.apache.ode.spi.compiler.Instructional;
import org.apache.ode.spi.compiler.Location;
import org.apache.ode.spi.compiler.ParserException;
import org.apache.ode.spi.compiler.ParserUtils;

public class AssignParser implements ElementParser<Contextual<Scope>> {
	public static final QName ASSIGN = new QName(BPEL.BPEL_EXEC_NAMESPACE, "assign");
	public static final QName COPY = new QName(BPEL.BPEL_EXEC_NAMESPACE, "copy");
	public static final QName TO = new QName(BPEL.BPEL_EXEC_NAMESPACE, "to");

	// QName varsName;

	public AssignParser(/*QName varsName*/) {
		// this.varsName = varsName;
	}

	@Override
	public void parse(XMLStreamReader input, Contextual<Scope> model, ExecCompilerContext context) throws XMLStreamException, ParserException {
		while (input.hasNext()) {
			int type = input.getEventType();
			switch (type) {
			case XMLStreamConstants.START_ELEMENT:
				ParserUtils.assertStart(input, ASSIGN);
				Contextual<Assign> assign = new Contextual<Assign>(ASSIGN, Assign.class, model);
				ParserUtils.setLocation(input, context.source().srcRef(), assign);
				model.children().add(assign);
				while (input.nextTag() == XMLStreamConstants.START_ELEMENT) {
					if (COPY.equals(input.getName())) {
						parseCopy(input, assign, context);
					} else {
						context.parseContent(input, assign);
					}
				}
				break;
			case XMLStreamConstants.END_ELEMENT:
				ParserUtils.assertEnd(input, ASSIGN);
				return;
			}
		}

	}

	public void parseCopy(XMLStreamReader input, Contextual<Assign> assign, ExecCompilerContext context) throws XMLStreamException, ParserException {
		while (input.hasNext()) {
			int type = input.getEventType();
			switch (type) {
			case XMLStreamConstants.START_ELEMENT:
				ParserUtils.assertStart(input, COPY);
				Contextual<Copy> copy = new Contextual<Copy>(COPY, Copy.class, assign);
				ParserUtils.setLocation(input, context.source().srcRef(), copy);
				assign.children().add(copy);
				boolean fromProcessed = false;
				while (input.nextTag() == XMLStreamConstants.START_ELEMENT) {
					if (FROM.equals(input.getName())) {
						parseFrom(input, copy, context);
						fromProcessed = true;
					} else if (TO.equals(input.getName())) {
						if (!fromProcessed) {
							context.addError(new Location(input.getLocation()), "out of order from/to", null);
							context.terminate();
						}
						Instructional<To> to = new Instructional<To>(TO, To.class, copy);
						ParserUtils.setLocation(input, context.source().srcRef(), to);
						String[] attributes = context.parseAttributes(input, to, "expressionLanguage", "variable", "part", "property", "partnerLink");
						to.instruction().setExpressionLanguage(attributes[0]);
						to.instruction().setVariable(attributes[1]);
						to.instruction().setPart(attributes[2]);
						if (attributes[3] != null) {
							to.instruction().setProperty(QName.valueOf(attributes[3]));
						}
						to.instruction().setPartnerLink(attributes[4]);
						copy.children().add(to);
						while (input.next() == XMLStreamConstants.START_ELEMENT || input.getEventType() == XMLStreamConstants.CHARACTERS) {
							if (input.getEventType() == XMLStreamConstants.CHARACTERS) {
								Value value = new Value();
								value.setValue(input.getText());
								to.instruction().setValue(value);
							} else if (QUERY.equals(input.getName())) {
								Query query = new Query();
								to.instruction().setQuery(query);
							} else {
								context.parseContent(input, to);
							}
						}
					} else {
						context.parseContent(input, copy);
					}
				}

				break;
			case XMLStreamConstants.END_ELEMENT:
				ParserUtils.assertEnd(input, COPY);
				return;
			}
		}
	}

}
