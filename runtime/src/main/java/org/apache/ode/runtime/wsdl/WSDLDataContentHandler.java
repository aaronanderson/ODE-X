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
package org.apache.ode.runtime.wsdl;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.ActivationDataFlavor;
import javax.activation.DataSource;
import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLLocator;
import javax.wsdl.xml.WSDLReader;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.ode.spi.repo.XMLDataContentHandler;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class WSDLDataContentHandler extends XMLDataContentHandler {

	public static final ActivationDataFlavor WSDL_FLAVOR = new ActivationDataFlavor(Definition.class, "application/wsdl; x-java-class=WSDL4J", "WSDL");

	private static final Logger log = Logger.getLogger(WSDLDataContentHandler.class.getName());
	
	@Override
	public Object getContent(DataSource dataSource) throws IOException {
		try {
			WSDLFactory factory = WSDLFactory.newInstance();
			WSDLReader reader = factory.newWSDLReader();
			return reader.readWSDL(new DSWSDLLocatorImpl(dataSource));
		} catch (WSDLException je) {
			throw new IOException(je);
		}
	}

	@Override
	public Object getTransferData(DataFlavor flavor, DataSource dataSource) throws UnsupportedFlavorException, IOException {
		if (Definition.class.equals(flavor.getDefaultRepresentationClass())) {
			try {
				WSDLFactory factory = WSDLFactory.newInstance();
				WSDLReader reader = factory.newWSDLReader();
				return reader.readWSDL(new DSWSDLLocatorImpl(dataSource));
			} catch (WSDLException je) {
				throw new IOException(je);
			}
		} else {
			return super.getTransferData(flavor, dataSource);
		}
	}

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[] { WSDL_FLAVOR, STREAM_FLAVOR, DOM_FLAVOR, XSL_SOURCE_FLAVOR };
	}

	@Override
	public byte[] toContent(Object content, String contentType) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		if (content instanceof Definition) {
			try {
				WSDLFactory factory = WSDLFactory.newInstance();
				WSDLWriter writer = factory.newWSDLWriter();
				writer.writeWSDL((Definition) content, bos);
				return bos.toByteArray();
			} catch (WSDLException je) {
				throw new IOException(je);
			}
		} else {
			return super.toContent(content, contentType);
		}
	}

	@Override
	public QName getDefaultQName(DataSource dataSource) {
		QName defaultName = null;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder db = factory.newDocumentBuilder();
			InputStream is = dataSource.getInputStream();
			Document doc = db.parse(is);
			XPathFactory xfactory = XPathFactory.newInstance();
			XPath xpath = xfactory.newXPath();
			xpath.setNamespaceContext(new NamespaceContext() {

				@Override
				public Iterator getPrefixes(String namespaceURI) {
					return null;
				}

				@Override
				public String getPrefix(String namespaceURI) {
					return null;
				}

				@Override
				public String getNamespaceURI(String prefix) {
					if ("wsdl".equals(prefix)) {
						return "http://schemas.xmlsoap.org/wsdl/";
					}
					return null;
				}
			});
			Attr nsAttr = (Attr) xpath.evaluate("/wsdl:definitions/@targetNamespace", doc, XPathConstants.NODE);
			Attr nameAttr = (Attr) xpath.evaluate("//wsdl:service[1]/@name", doc, XPathConstants.NODE);
			if (nsAttr != null && nameAttr != null) {
				defaultName = new QName(nsAttr.getNodeValue(), nameAttr.getNodeValue());
			}
			return defaultName;
		} catch (Exception e) {
			return null;
		}
	}

	class DSWSDLLocatorImpl implements WSDLLocator {
		DataSource dataSource;
		String lastResolved = null;

		public DSWSDLLocatorImpl(DataSource dataSource) {
			this.dataSource = dataSource;
		}

		@Override
		public void close() {

		}

		@Override
		public InputSource getBaseInputSource() {
			try {
				return new InputSource(dataSource.getInputStream());
			} catch (IOException e) {
				log.log(Level.SEVERE, "", e);
				return null;
			}
		}

		@Override
		public String getBaseURI() {
			return null;
		}

		@Override
		public InputSource getImportInputSource(String parentLocation, String importLocation) {
			return null;

		}

		@Override
		public String getLatestImportURI() {
			return lastResolved;
		}

	}

}
