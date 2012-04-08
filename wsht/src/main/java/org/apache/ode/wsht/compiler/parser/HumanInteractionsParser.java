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
package org.apache.ode.wsht.compiler.parser;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.ode.spi.compiler.Contextual;
import org.apache.ode.spi.compiler.ElementParser;
import org.apache.ode.spi.compiler.ExecCompilerContext;
import org.apache.ode.spi.compiler.ParserException;
import org.apache.ode.spi.compiler.ParserUtils;
import org.apache.ode.wsht.WSHT;
import org.apache.ode.wsht.exec.xml.HumanInteractions;

public class HumanInteractionsParser implements ElementParser<Contextual<HumanInteractions>> {
	public static final QName HUMAN_INTERACTIONS = new QName(WSHT.WSHT_NAMESPACE, "process");
	
	@Override
	public void parse(XMLStreamReader input, Contextual<HumanInteractions> model, ExecCompilerContext context) throws XMLStreamException, ParserException {
		while (input.hasNext()) {
			int type = input.getEventType();
			switch (type) {
			case XMLStreamConstants.START_ELEMENT:
				ParserUtils.assertStart(input, HUMAN_INTERACTIONS);
				ParserUtils.setLocation(input, context.source().srcRef(), model);
				String[] attrs = context.parseAttributes(input, model, "targetNamespace", "queryLanguage", "expressionLanguage");

				model.beginContext().setTargetNamespace(attrs[0]);
			
				 ParserUtils.skipChildren(input);
				 /*
				while (input.nextTag() == XMLStreamConstants.START_ELEMENT) {
					if (IMPORT.equals(input.getName())) {
						parseImport(input, model, context);
					} else {
						context.parseContent(input, model);
					}
				}*/
				break;
			case XMLStreamConstants.END_ELEMENT:
				ParserUtils.assertEnd(input, HUMAN_INTERACTIONS);
				return;
			}
		}

	}

	
}
