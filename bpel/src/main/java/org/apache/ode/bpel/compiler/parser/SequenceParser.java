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
import org.apache.ode.bpel.exec.xml.Sequence;
import org.apache.ode.spi.compiler.Contextual;
import org.apache.ode.spi.compiler.ElementParser;
import org.apache.ode.spi.compiler.ExecCompilerContext;
import org.apache.ode.spi.compiler.ParserException;
import org.apache.ode.spi.compiler.ParserUtils;

public class SequenceParser implements ElementParser<Contextual<Scope>> {
	public static final QName SEQUENCE = new QName(BPEL.BPEL_EXEC_NAMESPACE, "sequence");

	// QName varsName;

	public SequenceParser(/*QName varsName*/) {
		// this.varsName = varsName;
	}

	@Override
	public void parse(XMLStreamReader input, Contextual<Scope> model, ExecCompilerContext context) throws XMLStreamException, ParserException {
		while (input.hasNext()) {
			int type = input.getEventType();
			switch (type) {
			case XMLStreamConstants.START_ELEMENT:
				ParserUtils.assertStart(input, SEQUENCE);
				Contextual<Sequence> seq = new Contextual<Sequence>(SEQUENCE, Sequence.class, model);
				ParserUtils.setLocation(input, context.source().srcRef(), seq);
				model.children().add(seq);
				while (input.nextTag() == XMLStreamConstants.START_ELEMENT) {
					context.parseContent(input, seq);
				}
				break;
			case XMLStreamConstants.END_ELEMENT:
				ParserUtils.assertEnd(input, SEQUENCE);
				return;
			}
		}

	}
}