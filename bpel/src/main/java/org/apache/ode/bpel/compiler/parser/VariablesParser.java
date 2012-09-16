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
import org.apache.ode.bpel.exec.xml.From;
import org.apache.ode.bpel.exec.xml.From.Literal;
import org.apache.ode.bpel.exec.xml.From.Query;
import org.apache.ode.bpel.exec.xml.From.Value;
import org.apache.ode.bpel.exec.xml.Scope;
import org.apache.ode.bpel.exec.xml.Variable;
import org.apache.ode.bpel.exec.xml.Variables;
import org.apache.ode.spi.compiler.CompilerContext;
import org.apache.ode.spi.compiler.Contextual;
import org.apache.ode.spi.compiler.ElementParser;
import org.apache.ode.spi.compiler.ExecCompilerContext;
import org.apache.ode.spi.compiler.Instructional;
import org.apache.ode.spi.compiler.ParserException;
import org.apache.ode.spi.compiler.ParserUtils;
import org.apache.ode.spi.compiler.Unit;
import org.apache.ode.spi.exec.executable.xml.Instruction;

public class VariablesParser implements ElementParser<Contextual<Scope>> {
	public static final QName VARIABLES = new QName(BPEL.BPEL_EXEC_NAMESPACE, "variables");
	public static final QName VARIABLE = new QName(BPEL.BPEL_EXEC_NAMESPACE, "variable");
	public static final QName FROM = new QName(BPEL.BPEL_EXEC_NAMESPACE, "from");
	public static final QName LITERAL = new QName(BPEL.BPEL_EXEC_NAMESPACE, "literal");
	public static final QName QUERY = new QName(BPEL.BPEL_EXEC_NAMESPACE, "query");

	// QName varsName;

	public VariablesParser(/*QName varsName*/) {
		// this.varsName = varsName;
	}

	@Override
	public void parse(XMLStreamReader input, Contextual<Scope> model, ExecCompilerContext context) throws XMLStreamException, ParserException {
		while (input.hasNext()) {
			int type = input.getEventType();
			switch (type) {
			case XMLStreamConstants.START_ELEMENT:
				ParserUtils.assertStart(input, VARIABLES);
				Contextual<Variables> vars = new Contextual<Variables>(VARIABLES, Variables.class, model);
				ParserUtils.setLocation(input, context.source().srcRef(), vars);
				model.children().add(vars);
				while (input.nextTag() == XMLStreamConstants.START_ELEMENT) {
					if (VARIABLE.equals(input.getName())) {
						parseVariable(input, vars, context);
					} else {
						context.parseContent(input, vars);
					}
				}
				break;
			case XMLStreamConstants.END_ELEMENT:
				ParserUtils.assertEnd(input, VARIABLES);
				return;
			}
		}

	}

	public void parseVariable(XMLStreamReader input, Contextual<Variables> vars, ExecCompilerContext context) throws XMLStreamException, ParserException {
		while (input.hasNext()) {
			int type = input.getEventType();
			switch (type) {
			case XMLStreamConstants.START_ELEMENT:
				ParserUtils.assertStart(input, VARIABLE);
				if (ParserUtils.isContextual(input)) {
					Contextual<Variable> cvar = new Contextual<Variable>(VARIABLE, Variable.class, vars);
					vars.children().add(cvar);
					setVariableAttrs(input, cvar, context);
					ParserUtils.setLocation(input, context.source().srcRef(), cvar);
					while (input.getEventType() == XMLStreamConstants.START_ELEMENT) {
						if (FROM.equals(input.getName())) {
							parseFrom(input, cvar, context);
						} else {
							context.parseContent(input, cvar);
						}
						input.nextTag();
					}
				} else {
					Instructional<Variable> var = new Instructional<Variable>(VARIABLE, Variable.class, vars);
					ParserUtils.setLocation(input, context.source().srcRef(), var);
					setVariableAttrs(input, var, context);
					vars.children().add(var);
					while (input.nextTag() == XMLStreamConstants.START_ELEMENT) {
						context.parseContent(input, var);
					}
				}
				break;
			case XMLStreamConstants.END_ELEMENT:
				ParserUtils.assertEnd(input, VARIABLE);
				return;
			}
		}
	}

	public void setVariableAttrs(XMLStreamReader input, Unit<Variable> var, ExecCompilerContext context) throws ParserException, XMLStreamException {
		String[] attributes = context.parseAttributes(input, var, "name", "messageType", "type", "element");
		Variable ins = null;
		if (var instanceof Instructional) {
			ins = ((Instructional<Variable>) var).instruction();
		} else {
			ins = ((Contextual<Variable>) var).beginContext();
		}
		ins.setName(attributes[0]);
		if (attributes[1] != null) {
			ins.setMessageType(QName.valueOf(attributes[1]));
		}
		if (attributes[2] != null) {
			ins.setType(QName.valueOf(attributes[2]));
		}
		if (attributes[3] != null) {
			ins.setElement(QName.valueOf(attributes[3]));
		}

	}

	public static void parseFrom(XMLStreamReader input, Contextual<?> ctx, ExecCompilerContext context) throws XMLStreamException, ParserException {
		while (input.hasNext()) {
			int type = input.getEventType();
			switch (type) {
			case XMLStreamConstants.START_ELEMENT:
				ParserUtils.assertStart(input, FROM);
				Instructional<From> from = new Instructional<From>(FROM, From.class, ctx);
				ParserUtils.setLocation(input, context.source().srcRef(), from);
				String[] attributes = context.parseAttributes(input, from, "expressionLanguage", "variable", "part", "property", "partnerLink",
						"endpointReference");
				from.instruction().setExpressionLanguage(attributes[0]);
				from.instruction().setVariable(attributes[1]);
				from.instruction().setPart(attributes[2]);
				if (attributes[3] != null) {
					from.instruction().setProperty(QName.valueOf(attributes[3]));
				}
				from.instruction().setPartnerLink(attributes[4]);
				from.instruction().setEndpointReference(attributes[5]);
				ctx.children().add(from);
				while (input.next() == XMLStreamConstants.START_ELEMENT || input.getEventType() == XMLStreamConstants.CHARACTERS) {
					if (input.getEventType() == XMLStreamConstants.CHARACTERS) {
						Value value = new Value();
						value.setValue(input.getText());
						from.instruction().setValue(value);
					} else if (LITERAL.equals(input.getName())) {
						Literal literal = new Literal();
						from.instruction().setLiteral(literal);
					} else if (QUERY.equals(input.getName())) {
						Query query = new Query();
						from.instruction().setQuery(query);
					} else {
						context.parseContent(input, from);
					}
				}

				break;
			case XMLStreamConstants.END_ELEMENT:
				ParserUtils.assertEnd(input, FROM);
				return;
			}
		}
	}

}
