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
import org.apache.ode.bpel.exec.xml.PartnerLink;
import org.apache.ode.bpel.exec.xml.PartnerLinks;
import org.apache.ode.bpel.exec.xml.Scope;
import org.apache.ode.spi.compiler.CompilerContext;
import org.apache.ode.spi.compiler.Contextual;
import org.apache.ode.spi.compiler.ElementParser;
import org.apache.ode.spi.compiler.ExecCompilerContext;
import org.apache.ode.spi.compiler.Instructional;
import org.apache.ode.spi.compiler.ParserException;
import org.apache.ode.spi.compiler.ParserUtils;

public class PartnerLinksParser implements ElementParser<Contextual<Scope>> {
	public static final QName PARTNERLINKS = new QName(BPEL.BPEL_EXEC_NAMESPACE, "partnerLinks");
	public static final QName PARTNERLINK = new QName(BPEL.BPEL_EXEC_NAMESPACE, "partnerLink");

	QName varsName;

	public PartnerLinksParser(/*QName varsName*/) {
		// this.varsName = varsName;
	}

	@Override
	public void parse(XMLStreamReader input, Contextual<Scope> model, ExecCompilerContext context) throws XMLStreamException, ParserException {
		while (input.hasNext()) {
			int type = input.getEventType();
			switch (type) {
			case XMLStreamConstants.START_ELEMENT:
				ParserUtils.assertStart(input, PARTNERLINKS);
				Contextual<PartnerLinks> links = new Contextual<PartnerLinks>(PARTNERLINKS, PartnerLinks.class, model);
				ParserUtils.setLocation(input, context.source().srcRef(), links);
				model.children().add(links);
				while (input.nextTag() == XMLStreamConstants.START_ELEMENT) {
					if (PARTNERLINK.equals(input.getName())) {
						parsePartnerLink(input, links, context);
					} else {
						context.parseContent(input, links);
					}
				}
				break;
			case XMLStreamConstants.END_ELEMENT:
				ParserUtils.assertEnd(input, PARTNERLINKS);
				return;
			}
		}

	}

	public void parsePartnerLink(XMLStreamReader input, Contextual<PartnerLinks> links, ExecCompilerContext context) throws XMLStreamException, ParserException {
		while (input.hasNext()) {
			int type = input.getEventType();
			switch (type) {
			case XMLStreamConstants.START_ELEMENT:
				ParserUtils.assertStart(input, PARTNERLINK);
				Instructional<PartnerLink> link = new Instructional<PartnerLink>(PARTNERLINK, PartnerLink.class, links);
				ParserUtils.setLocation(input, context.source().srcRef(), link);
				links.children().add(link);
				while (input.nextTag() == XMLStreamConstants.START_ELEMENT) {
					context.parseContent(input, link);
				}
				break;
			case XMLStreamConstants.END_ELEMENT:
				ParserUtils.assertEnd(input, PARTNERLINK);
				return;
			}
		}
	}

}
