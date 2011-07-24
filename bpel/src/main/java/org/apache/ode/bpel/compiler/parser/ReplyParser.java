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
import org.apache.ode.bpel.exec.xml.Receive;
import org.apache.ode.bpel.exec.xml.Reply;
import org.apache.ode.bpel.exec.xml.Scope;
import org.apache.ode.bpel.exec.xml.ToPart;
import org.apache.ode.bpel.exec.xml.ToParts;
import org.apache.ode.spi.compiler.CompilerContext;
import org.apache.ode.spi.compiler.Contextual;
import org.apache.ode.spi.compiler.Instructional;
import org.apache.ode.spi.compiler.ElementParser;
import org.apache.ode.spi.compiler.ParserException;
import org.apache.ode.spi.compiler.ParserUtils;

import static org.apache.ode.bpel.compiler.parser.ReceiveParser.CORRELATIONS;
import static org.apache.ode.bpel.compiler.parser.ReceiveParser.parseCorrelations;

public class ReplyParser implements ElementParser<Contextual<Scope>> {
	public static final QName REPLY = new QName(BPEL.BPEL_EXEC_NAMESPACE, "reply");
	public static final QName TOPARTS = new QName(BPEL.BPEL_EXEC_NAMESPACE, "toParts");
	public static final QName TOPART = new QName(BPEL.BPEL_EXEC_NAMESPACE, "toPart");

	// QName varsName;

	public ReplyParser(/*QName varsName*/) {
		// this.varsName = varsName;
	}

	@Override
	public void parse(XMLStreamReader input, Contextual<Scope> model, CompilerContext context) throws XMLStreamException, ParserException {
		while (input.hasNext()) {
			int type = input.getEventType();
			switch (type) {
			case XMLStreamConstants.START_ELEMENT:
				ParserUtils.assertStart(input, REPLY);
				Reply ins = new Reply();
				if (ParserUtils.isContextual(input)) {
					Contextual<Reply> reply = new Contextual<Reply>(REPLY, ins, new Reply(), model);
					model.children().add(reply);
					ParserUtils.setLocation(input, context.source().srcRef(), reply);
					while (input.nextTag() == XMLStreamConstants.START_ELEMENT) {
						if (CORRELATIONS.equals(input.getName())) {
							parseCorrelations(input, reply, context);
						} else if (TOPARTS.equals(input.getName())) {
							parseToParts(input, reply, context);
						} else {
							context.parseContent(input, reply);
						}
					}
				} else {
					Instructional<Reply> reply = new Instructional<Reply>(REPLY, ins, model);
					ParserUtils.setLocation(input, context.source().srcRef(), reply);
					model.children().add(reply);
					while (input.nextTag() == XMLStreamConstants.START_ELEMENT) {
						context.parseContent(input, reply);
					}
				}
				break;
			case XMLStreamConstants.END_ELEMENT:
				ParserUtils.assertEnd(input, REPLY);
				return;
			}
		}

	}

	public void parseToParts(XMLStreamReader input, Contextual<Reply> reply, CompilerContext context) throws XMLStreamException, ParserException {
		while (input.hasNext()) {
			int type = input.getEventType();
			switch (type) {
			case XMLStreamConstants.START_ELEMENT:
				ParserUtils.assertStart(input, TOPARTS);
				Contextual<ToParts> toParts = new Contextual<ToParts>(TOPARTS, ToParts.class, reply);
				ParserUtils.setLocation(input, context.source().srcRef(), toParts);
				reply.children().add(toParts);
				while (input.nextTag() == XMLStreamConstants.START_ELEMENT) {
					if (TOPART.equals(input.getName())) {
						Instructional<ToPart> toPart = new Instructional<ToPart>(TOPART, ToPart.class, toParts);
						ParserUtils.setLocation(input, context.source().srcRef(), toPart);
						toParts.children().add(toPart);
						while (input.nextTag() == XMLStreamConstants.START_ELEMENT) {
							context.parseContent(input, toPart);
						}
					} else {
						context.parseContent(input, toParts);
					}
				}
				break;
			case XMLStreamConstants.END_ELEMENT:
				ParserUtils.assertEnd(input, TOPARTS);
				return;
			}
		}
	}

}
