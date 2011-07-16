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
import org.apache.ode.bpel.compiler.model.ExecutableProcessModel;
import org.apache.ode.spi.compiler.CompilerContext;
import org.apache.ode.spi.compiler.Parser;
import org.apache.ode.spi.compiler.ParserException;
import org.apache.ode.spi.compiler.ParserUtils;

public class ExecutableProcessParser implements Parser<ExecutableProcessModel> {
	public static final QName EXECUTABLE = new QName(BPEL.BPEL_EXEC_NAMESPACE, "process");

	@Override
	public void parse(XMLStreamReader input, ExecutableProcessModel model, CompilerContext context) throws XMLStreamException, ParserException {
		while (input.hasNext()) {
			int type = input.getEventType();
			switch (type) {
			case XMLStreamConstants.START_ELEMENT:
				ParserUtils.assertStart(input, EXECUTABLE);
				ParserUtils.startContext(input, context.source().id(), model.getStartProcess());
				ParserUtils.endContext(input, context.source().id(), model.getEndProcess());
				model.getStartProcess().setQueryLanguage(input.getAttributeValue(BPEL.BPEL_EXEC_NAMESPACE, "queryLanguage"));
				model.getStartProcess().setExpressionLanguage(input.getAttributeValue(BPEL.BPEL_EXEC_NAMESPACE, "expressionLanguage"));
				model.getBlock().getBody().add(model.getStartProcess());
				ParserUtils.skip(input);
				break;

			case XMLStreamConstants.END_ELEMENT:
				ParserUtils.assertEnd(input, EXECUTABLE);
				model.getBlock().getBody().add(model.getEndProcess());
				return;
			}
		}

	}

}
