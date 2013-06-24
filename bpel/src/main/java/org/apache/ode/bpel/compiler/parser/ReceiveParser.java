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
import org.apache.ode.bpel.exec.xml.Correlation;
import org.apache.ode.bpel.exec.xml.Correlations;
import org.apache.ode.bpel.exec.xml.FromPart;
import org.apache.ode.bpel.exec.xml.FromParts;
import org.apache.ode.bpel.exec.xml.Receive;
import org.apache.ode.bpel.exec.xml.Scope;
import org.apache.ode.spi.compiler.CompilerContext;
import org.apache.ode.spi.compiler.Contextual;
import org.apache.ode.spi.compiler.ElementParser;
import org.apache.ode.spi.compiler.ExecCompilerContext;
import org.apache.ode.spi.compiler.Instructional;
import org.apache.ode.spi.compiler.ParserException;
import org.apache.ode.spi.compiler.ParserUtils;

public class ReceiveParser implements ElementParser<Contextual<Scope>> {
	public static final QName RECEIVE = new QName(BPEL.BPEL_EXEC_NAMESPACE, "receive");
	public static final QName CORRELATION = new QName(BPEL.BPEL_EXEC_NAMESPACE, "correlations");
	public static final QName CORRELATIONS = new QName(BPEL.BPEL_EXEC_NAMESPACE, "correlation");
	public static final QName FROMPARTS = new QName(BPEL.BPEL_EXEC_NAMESPACE, "fromParts");
	public static final QName FROMPART = new QName(BPEL.BPEL_EXEC_NAMESPACE, "fromPart");

	// QName varsName;

	public ReceiveParser(/*QName varsName*/) {
		// this.varsName = varsName;
	}

	@Override
	public void parse(XMLStreamReader input, Contextual<Scope> model, ExecCompilerContext context) throws XMLStreamException, ParserException {
		while (input.hasNext()) {
			int type = input.getEventType();
			switch (type) {
			case XMLStreamConstants.START_ELEMENT:
				ParserUtils.assertStart(input, RECEIVE);
				Receive ins = new Receive();
				if (ParserUtils.isContextual(input)) {
					Contextual<Receive> receive = new Contextual<Receive>(RECEIVE, ins, new Receive(), model);
					model.children().add(receive);
					ParserUtils.setLocation(input, context.source().srcRef(), receive);
					while (input.nextTag() == XMLStreamConstants.START_ELEMENT) {
						if (CORRELATIONS.equals(input.getName())) {
							parseCorrelations(input, receive, context);
						} else if (FROMPARTS.equals(input.getName())) {
							parseFromParts(input, receive, context);
						} else {
							context.parseContent(input, receive);
						}
						input.nextTag();
					}
				} else {
					Instructional<Receive> receive = new Instructional<Receive>(RECEIVE, ins, model);
					ParserUtils.setLocation(input, context.source().srcRef(), receive);
					model.children().add(receive);
					while (input.nextTag() == XMLStreamConstants.START_ELEMENT) {
						context.parseContent(input, receive);
					}
				}

				break;
			case XMLStreamConstants.END_ELEMENT:
				ParserUtils.assertEnd(input, RECEIVE);
				return;
			}
		}

	}

	public static void parseCorrelations(XMLStreamReader input, Contextual<?> ctx, ExecCompilerContext context) throws XMLStreamException, ParserException {
		while (input.hasNext()) {
			int type = input.getEventType();
			switch (type) {
			case XMLStreamConstants.START_ELEMENT:
				ParserUtils.assertStart(input, CORRELATIONS);
				Contextual<Correlations> corrs = new Contextual<Correlations>(CORRELATIONS, Correlations.class, ctx);
				ParserUtils.setLocation(input, context.source().srcRef(), corrs);
				ctx.children().add(corrs);
				while (input.nextTag() == XMLStreamConstants.START_ELEMENT) {
					if (CORRELATION.equals(input.getName())) {
						Instructional<Correlation> corr = new Instructional<Correlation>(CORRELATION, Correlation.class, corrs);
						ParserUtils.setLocation(input, context.source().srcRef(), corr);
						corrs.children().add(corr);
						while (input.nextTag() == XMLStreamConstants.START_ELEMENT) {
							context.parseContent(input, corr);
						}
					} else {
						context.parseContent(input, corrs);
					}
				}

				break;
			case XMLStreamConstants.END_ELEMENT:
				ParserUtils.assertEnd(input, CORRELATIONS);
				return;
			}
		}
	}

	public void parseFromParts(XMLStreamReader input, Contextual<Receive> receive, ExecCompilerContext context) throws XMLStreamException, ParserException {
		while (input.hasNext()) {
			int type = input.getEventType();
			switch (type) {
			case XMLStreamConstants.START_ELEMENT:
				ParserUtils.assertStart(input, FROMPARTS);
				Contextual<FromParts> fromParts = new Contextual<FromParts>(FROMPARTS, FromParts.class, receive);
				ParserUtils.setLocation(input, context.source().srcRef(), fromParts);
				receive.children().add(fromParts);
				while (input.nextTag() == XMLStreamConstants.START_ELEMENT) {
					if (FROMPART.equals(input.getName())) {
						Instructional<FromPart> fromPart = new Instructional<FromPart>(FROMPART, FromPart.class, fromParts);
						ParserUtils.setLocation(input, context.source().srcRef(), fromPart);
						fromParts.children().add(fromPart);
						while (input.nextTag() == XMLStreamConstants.START_ELEMENT) {
							context.parseContent(input, fromPart);
						}
					} else {
						context.parseContent(input, fromParts);
					}
				}
				break;
			case XMLStreamConstants.END_ELEMENT:
				ParserUtils.assertEnd(input, FROMPARTS);
				return;
			}
		}
	}

}
