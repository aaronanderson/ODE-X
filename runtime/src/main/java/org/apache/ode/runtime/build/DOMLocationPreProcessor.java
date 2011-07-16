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
package org.apache.ode.runtime.build;

import java.io.ByteArrayInputStream;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.ode.spi.compiler.ParserException;
import org.apache.ode.spi.exec.xml.Source;
import org.apache.ode.spi.exec.xml.SourceRef;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

/**
 * As part of the source pre-process phase we need to inline the XML location data into the DOM so we don't lose original source data after performing any
 * additional transformations on it.
 */
public class DOMLocationPreProcessor {



	/* Ditched this because SAX gives the location right >after< the element not right before it as needed
	public static Document inlineLocation(byte[] content) throws ParserException {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.newDocument();
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser parser = spf.newSAXParser();
			parser.parse(new ByteArrayInputStream(content), new DOMLocationHandler(doc));
			return doc;
		} catch (Exception e) {
			throw new ParserException(e);
		}
	}

	public static class DOMLocationHandler extends DefaultHandler {
		final Stack<Element> stack = new Stack<Element>();
		Locator locator;
		final Document doc;
		final StringBuilder textBuffer = new StringBuilder();

		DOMLocationHandler(Document doc) {
			this.doc = doc;
		}

		@Override
		public void setDocumentLocator(Locator locator) {
			this.locator = locator;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			appendText();
			Element el = doc.createElement(qName);
			for (int i = 0; i < attributes.getLength(); i++) {
				el.setAttribute(attributes.getQName(i), attributes.getValue(i));
			}
			if (stack.isEmpty()) {// root
				el.setAttribute("xmlns:" + LOCATION_NS_PREFIX, LOCATION_NS);
			}
			el.setAttribute(LOCATION_NS_PREFIX + ":" + LOCATION_START_LINE_ATTR, String.valueOf(locator.getLineNumber()));
			el.setAttribute(LOCATION_NS_PREFIX + ":" + LOCATION_START_COL_ATTR, String.valueOf(locator.getColumnNumber()));
			stack.push(el);
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			appendText();
			Element top = stack.pop();
			top.setAttribute(LOCATION_NS_PREFIX + ":" + LOCATION_END_LINE_ATTR, String.valueOf(locator.getLineNumber()));
			top.setAttribute(LOCATION_NS_PREFIX + ":" + LOCATION_END_COL_ATTR, String.valueOf(locator.getColumnNumber()));
			if (stack.isEmpty()) {// root
				doc.appendChild(top);
			} else {
				Element parent = stack.peek();
				parent.appendChild(top);
			}
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			textBuffer.append(ch, start, length);
		}

		void appendText() {
			if (textBuffer.length() > 0) {
				if (!stack.isEmpty()) {// ignore text/whitespaces before root
					Element top = stack.peek();
					Text text = doc.createTextNode(textBuffer.toString());
					top.appendChild(text);
				}
				textBuffer.delete(0, textBuffer.length());

			}
		}

	}*/
}
