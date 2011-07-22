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
import org.apache.ode.bpel.exec.xml.Scope;
import org.apache.ode.bpel.exec.xml.Variable;
import org.apache.ode.bpel.exec.xml.Variables;
import org.apache.ode.spi.compiler.CompilerContext;
import org.apache.ode.spi.compiler.Contextual;
import org.apache.ode.spi.compiler.Instructional;
import org.apache.ode.spi.compiler.Parser;
import org.apache.ode.spi.compiler.ParserException;
import org.apache.ode.spi.compiler.ParserUtils;
import org.apache.ode.spi.exec.xml.Context;

public class VariablesParser implements Parser<Contextual<Scope>> {
	public static final QName VARIABLES = new QName(BPEL.BPEL_EXEC_NAMESPACE, "variables");
	public static final QName VARIABLE = new QName(BPEL.BPEL_EXEC_NAMESPACE, "variable");

	// QName varsName;

	public VariablesParser(/*QName varsName*/) {
		// this.varsName = varsName;
	}

	@Override
	public void parse(XMLStreamReader input, Contextual<Scope> model, CompilerContext context) throws XMLStreamException, ParserException {
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

	public void parseVariable(XMLStreamReader input, Contextual<Variables> vars, CompilerContext context) throws XMLStreamException, ParserException {
		while (input.hasNext()) {
			int type = input.getEventType();
			switch (type) {
			case XMLStreamConstants.START_ELEMENT:
				ParserUtils.assertStart(input, VARIABLE);
				Instructional<Variable> var = new Instructional<Variable>(VARIABLE, Variable.class, vars);
				ParserUtils.setLocation(input, context.source().srcRef(), var);
				vars.children().add(var);
				while (input.nextTag() == XMLStreamConstants.START_ELEMENT) {
					context.parseContent(input, var);
				}
				break;
			case XMLStreamConstants.END_ELEMENT:
				ParserUtils.assertEnd(input, VARIABLE);
				return;
			}
		}
	}

}
