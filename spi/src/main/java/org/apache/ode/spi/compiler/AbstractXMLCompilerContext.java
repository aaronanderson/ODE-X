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
package org.apache.ode.spi.compiler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.ode.spi.xml.AttributeHandler;
import org.apache.ode.spi.xml.ElementHandler;
import org.apache.ode.spi.xml.HandlerException;
import org.apache.ode.spi.xml.HandlerRegistry;

public abstract class AbstractXMLCompilerContext<U, X extends HandlerException, C extends XMLCompilerContext<U, X>, E extends ElementHandler<? extends U, C, X>, A extends AttributeHandler<? extends U, C, X>>
		extends AbstractCompilerContext<U> implements XMLCompilerContext<U, X> {

	// <U, ? extends XMLCompilerContextImpl<U>, ElementHandler<? extends U, ? extends XMLCompilerContextImpl<U>>, AttributeHandler<? extends U, ? extends
	// XMLCompilerContextImpl<U>>>
	private AbstractHandlerRegistry<U, X,C, E, A> registry;

	public void init(Source src, Compilation state, AbstractHandlerRegistry registry) {
		super.init(src, state);
		this.registry = registry;
	}

	@Override
	public <M extends U> void parseContent(XMLStreamReader input, M subModel) throws XMLStreamException, X {
		if (input.getEventType() != XMLStreamConstants.START_ELEMENT) {
			return;
		}

		ElementHandler<M, C, X> handler = (ElementHandler<M, C, X>) registry.retrieve(input.getName(), subModel);
		if (handler != null) {
				handler.parse(input, subModel, (C) this);
		} else {
			throw registry.convertException(new HandlerException(String.format("[%d,%d] Unable to locate handler for %s", input.getLocation().getLineNumber(), input
					.getLocation().getColumnNumber(), input.getName())));

		}
	}

	@Override
	public <M extends U> String[] parseAttributes(XMLStreamReader input, M subModel, String... attrName) throws XMLStreamException, X {
		if (input.getEventType() != XMLStreamConstants.START_ELEMENT) {
			return null;
		}
		String[] results = null;
		Map<String, Integer> attrNames = new HashMap<String, Integer>();
		if (attrName != null) {
			results = new String[attrName.length];
			for (int i = 0; i < attrName.length; i++) {
				attrNames.put(attrName[i], i);
			}
		}
		for (int i = 0; i < input.getAttributeCount(); i++) {
			if (ParserUtils.LOCATION_NS.equals(input.getAttributeNamespace(i))) {
				continue;
			}
			boolean handled = false;
			for (Iterator<Map.Entry<String, Integer>> j = attrNames.entrySet().iterator(); j.hasNext();) {
				Map.Entry<String, Integer> reqAttrName = j.next();
				if (reqAttrName.getKey().equals(input.getAttributeLocalName(i))) {
					String attrNS = input.getAttributeNamespace(i);
					if (attrNS == null || attrNS.length() == 0 || attrNS.equals(input.getNamespaceURI())) {
						j.remove();
						results[reqAttrName.getValue()] = input.getAttributeValue(i);
						handled = true;
						break;
					}

				}
			}
			if (!handled) {
				AttributeHandler<M, C, X> handler = (AttributeHandler<M, C, X>) registry.retrieve(input.getName(),
						input.getAttributeName(i), subModel);
				if (handler != null) {
						handler.parse(input.getName(), input.getAttributeName(i), input.getAttributeValue(i), subModel, (C) this);
				}
			}
		}
		return results;
	}

	

}
