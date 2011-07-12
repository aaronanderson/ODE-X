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
package org.apache.ode.spi.compiler;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stax.StAXSource;

import org.apache.ode.spi.exec.xml.SourceRef;
import org.w3c.dom.Element;

public abstract class ParserUtils {

	public static void configureSource(XMLStreamReader input, SourceRef src) throws XMLStreamException {
		Location location = input.getLocation();
		src.setLine(location.getLineNumber());
		src.setColumn(location.getColumnNumber());
	}

	public static void assertStart(XMLStreamReader input, QName element) throws ParserException {
		if (input.getEventType() != XMLStreamConstants.START_ELEMENT || !element.equals(input.getName())) {
			throw new ParserException("Start element assertion failed");
		}

	}

	public static void assertEnd(XMLStreamReader input, QName element) throws ParserException {
		if (input.getEventType() != XMLStreamConstants.END_ELEMENT || !element.equals(input.getName())) {
			throw new ParserException("End element assertion failed");
		}

	}

	public static void skip(XMLStreamReader input) throws XMLStreamException, ParserException {
		if (input.getEventType() != XMLStreamConstants.START_ELEMENT) {
			throw new ParserException("reader must be in the start element state");
		}
		int level = 1;
		while (input.hasNext()) {
			int type = input.next();
			if (type == XMLStreamConstants.START_ELEMENT) {
				level++;
			} else if (type == XMLStreamConstants.END_ELEMENT) {
				if (--level == 0) {
					return;
				}
			}
		}
	}

	public static void skipTo(XMLStreamReader input, Location location) throws XMLStreamException {

		while (input.hasNext()) {
			if (input.getLocation().getCharacterOffset() == location.getCharacterOffset()) {
				break;
			}
			input.next();
		}

	}

	public static void skipTo(XMLStreamReader input, org.apache.ode.spi.compiler.Location location) throws XMLStreamException {

		if (location.getOffset() != -1) {
			while (input.hasNext()) {
				if (input.getLocation().getCharacterOffset() == location.getOffset()) {
					break;
				}
				input.next();
			}
		} else {
			while (input.hasNext()) {
				if (input.getLocation().getLineNumber() == location.getLine() && input.getLocation().getColumnNumber() == location.getColumn()) {
					break;
				}
				input.next();
			}
		}

	}

	// TODO need a skip to location so that location can be preserved on inlined content
	public static Element extract(XMLStreamReader input) throws ParserException {
		try {
			TransformerFactory transformFactory = TransformerFactory.newInstance();
			Transformer tform = transformFactory.newTransformer();
			DOMResult result = new DOMResult();
			tform.transform(new StAXSource(input), result);
			return (Element) result.getNode();
		} catch (Exception e) {
			throw new ParserException(e);
		}
	}

}
