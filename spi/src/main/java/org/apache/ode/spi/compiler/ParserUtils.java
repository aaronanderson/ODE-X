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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.ode.spi.exec.xml.Context;
import org.apache.ode.spi.exec.xml.ContextMode;
import org.apache.ode.spi.exec.xml.SourceRef;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

public class ParserUtils {

	public static final String LOCATION_NS = "http://ode.apache.org/compiler/location";
	public static final String LOCATION_NS_PREFIX = "ol";
	public static final String LOCATION_START_LINE_ATTR = "sl";
	public static final String LOCATION_START_COL_ATTR = "sc";
	public static final String LOCATION_END_LINE_ATTR = "el";
	public static final String LOCATION_END_COL_ATTR = "ec";

	public static final String LOCATION_START_LINE_NSATTR = LOCATION_NS_PREFIX + ":" + LOCATION_START_LINE_ATTR;
	public static final String LOCATION_START_COL_NSATTR = LOCATION_NS_PREFIX + ":" + LOCATION_START_COL_ATTR;
	public static final String LOCATION_END_LINE_NSATTR = LOCATION_NS_PREFIX + ":" + LOCATION_END_LINE_ATTR;
	public static final String LOCATION_END_COL_NSATTR = LOCATION_NS_PREFIX + ":" + LOCATION_END_COL_ATTR;

	public static void setLocation(XMLStreamReader input, org.apache.ode.spi.exec.xml.Source src, SourceRef srcRef) throws XMLStreamException {
		srcRef.setSref(src);
		srcRef.setLine(Integer.parseInt(input.getAttributeValue(LOCATION_NS, LOCATION_END_LINE_ATTR)));
		srcRef.setColumn(Integer.parseInt(input.getAttributeValue(LOCATION_NS, LOCATION_END_COL_ATTR)));
	}

	public static void startContext(XMLStreamReader input, org.apache.ode.spi.exec.xml.Source src, Context ctx) throws XMLStreamException {
		ctx.setSref(src);
		ctx.setLine(Integer.parseInt(input.getAttributeValue(LOCATION_NS, LOCATION_START_LINE_ATTR)));
		ctx.setColumn(Integer.parseInt(input.getAttributeValue(LOCATION_NS, LOCATION_START_COL_ATTR)));
	}

	public static void endContext(XMLStreamReader input, org.apache.ode.spi.exec.xml.Source src, Context ctx) throws XMLStreamException {
		ctx.setSref(src);
		ctx.setMode(ContextMode.END);
		ctx.setLine(Integer.parseInt(input.getAttributeValue(LOCATION_NS, LOCATION_END_LINE_ATTR)));
		ctx.setColumn(Integer.parseInt(input.getAttributeValue(LOCATION_NS, LOCATION_END_COL_ATTR)));
	}

	public static Document inlineLocation(byte[] content) throws ParserException {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			DocumentBuilder db = dbf.newDocumentBuilder();
			DOMImplementation impl = db.getDOMImplementation();
			Document doc = null;
			XMLInputFactory inputFactory = XMLInputFactory.newInstance();
			XMLStreamReader reader = inputFactory.createXMLStreamReader(new ByteArrayInputStream(content));
			Stack<Element> stack = new Stack<Element>();
			StringBuilder textBuffer = new StringBuilder();
			while (reader.hasNext()) {
				int type = reader.next();
				switch (type) {
				case XMLStreamConstants.START_ELEMENT:
					if (textBuffer.length() > 0) {
						if (!stack.isEmpty()) {// ignore text/whitespaces before root
							Element top = stack.peek();
							Text text = doc.createTextNode(textBuffer.toString());
							top.appendChild(text);
						}
						textBuffer.delete(0, textBuffer.length());
					}
					Element el = null;
					if (doc == null) {
						doc = impl.createDocument(reader.getNamespaceURI(), reader.getLocalName(), null);
						el = doc.getDocumentElement();
					} else {
						el = doc.createElement(reader.getPrefix().length() > 0 ? reader.getPrefix() + ":" + reader.getLocalName() : reader.getLocalName());
					}
					for (int i = 0; i < reader.getNamespaceCount(); i++) {
						if (reader.getNamespacePrefix(i).length() > 0) {
							el.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:" + reader.getNamespacePrefix(i), reader.getNamespaceURI(i));
						}
					}
					for (int i = 0; i < reader.getAttributeCount(); i++) {
						if (reader.getAttributePrefix(i).length() > 0) {
							el.setAttributeNS(reader.getAttributeNamespace(i), reader.getAttributePrefix(i) + ":" + reader.getAttributeLocalName(i),
									reader.getAttributeValue(i));
						} else {
							el.setAttribute(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
						}

					}
					el.setAttributeNS(LOCATION_NS, LOCATION_START_LINE_NSATTR, String.valueOf(reader.getLocation().getLineNumber()));
					el.setAttributeNS(LOCATION_NS, LOCATION_START_COL_NSATTR, String.valueOf(reader.getLocation().getColumnNumber()));
					stack.push(el);
					break;

				case XMLStreamConstants.END_ELEMENT:
					if (textBuffer.length() > 0) {
						if (!stack.isEmpty()) {// ignore text/whitespaces before root
							Element top = stack.peek();
							Text text = doc.createTextNode(textBuffer.toString());
							top.appendChild(text);
						}
						textBuffer.delete(0, textBuffer.length());

					}
					Element top = stack.pop();
					String endLine = String.valueOf(reader.getLocation().getLineNumber());
					String endColumn = String.valueOf(reader.getLocation().getColumnNumber());
					String startLine = top.getAttributeNS(LOCATION_NS, LOCATION_START_LINE_NSATTR);
					String startColumn = top.getAttributeNS(LOCATION_NS, LOCATION_START_COL_NSATTR);
					if (!startLine.equals(endLine) || !startColumn.equals(endColumn)) {
						top.setAttributeNS(LOCATION_NS, LOCATION_END_LINE_NSATTR, endLine);
						top.setAttributeNS(LOCATION_NS, LOCATION_END_COL_NSATTR, endColumn);
					}
					if (!stack.isEmpty()) {
						Element parent = stack.peek();
						parent.appendChild(top);
					}
					break;

				case XMLStreamConstants.CHARACTERS:
					textBuffer.append(reader.getText());
					break;
				}
			}
			return doc;
			/* transform to new namespace aware doc so XPath pre-process operations will work
			TransformerFactory transformFactory = TransformerFactory.newInstance();
			Transformer tform = transformFactory.newTransformer();
			dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			db = dbf.newDocumentBuilder();
			DOMResult dr = new DOMResult(db.newDocument());
			tform.transform(new DOMSource(doc), dr);
			return (Document) dr.getNode();*/
		} catch (Exception e) {
			throw new ParserException(e);
		}
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
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.newDocument();
			DOMResult result = new DOMResult(doc);
			tform.transform(new StAXSource(input), result);
			return (Element) result.getNode();
		} catch (Exception e) {
			throw new ParserException(e);
		}
	}

	public static String domToString(Document doc) throws ParserException {
		try {
			TransformerFactory transformFactory = TransformerFactory.newInstance();
			Transformer tform = transformFactory.newTransformer();
			tform.setOutputProperty(OutputKeys.INDENT, "yes");
			StringWriter writer = new StringWriter();
			tform.transform(new DOMSource(doc), new StreamResult(writer));
			return writer.toString();
		} catch (Exception e) {
			throw new ParserException(e);
		}
	}

	public static byte[] domToContent(Document doc) throws ParserException {
		try {
			TransformerFactory transformFactory = TransformerFactory.newInstance();
			Transformer tform = transformFactory.newTransformer();
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			tform.transform(new DOMSource(doc), new StreamResult(bos));
			return bos.toByteArray();
		} catch (Exception e) {
			throw new ParserException(e);
		}
	}

	public static Document contentToDom(byte[] content) throws ParserException {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			DocumentBuilder db = dbf.newDocumentBuilder();
			return db.parse(new ByteArrayInputStream(content));
		} catch (Exception e) {
			throw new ParserException(e);
		}
	}

	public static NamespaceContext buildNSContext(final Map<String, String> prefixMappings) {
		final Map<String, String> nsMappings = new HashMap<String, String>();
		for (Map.Entry<String, String> e : prefixMappings.entrySet()) {
			nsMappings.put(e.getValue(), e.getKey());
		}

		return new NamespaceContext() {

			@Override
			public Iterator getPrefixes(String namespaceURI) {
				return null;
			}

			@Override
			public String getPrefix(String namespaceURI) {
				return nsMappings.get(namespaceURI);
			}

			@Override
			public String getNamespaceURI(String prefix) {
				return prefixMappings.get(prefix);
			}
		};
	}

}