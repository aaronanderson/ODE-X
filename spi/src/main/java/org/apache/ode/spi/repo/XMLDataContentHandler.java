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
package org.apache.ode.spi.repo;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.activation.ActivationDataFlavor;
import javax.activation.DataSource;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;

public class XMLDataContentHandler extends DataContentHandler {
	public static final ActivationDataFlavor STREAM_FLAVOR = new ActivationDataFlavor(XMLStreamReader.class, "application/xml; x-java-class=XMLStreamReader",
			"Stax XML Stream Reader");
	public static final ActivationDataFlavor DOM_FLAVOR = new ActivationDataFlavor(Document.class, "application/xml; x-java-class=W3CDom", "W3C DOM");
	public static final ActivationDataFlavor XSL_SOURCE_FLAVOR = new ActivationDataFlavor(StreamSource.class, "application/xml; x-java-class=SrreamSource","XSL Stream Source");

	@Override
	public Object getContent(DataSource dataSource) throws IOException {
		try {
			return XMLInputFactory.newInstance().createXMLStreamReader(dataSource.getInputStream());
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	@Override
	public Object getTransferData(DataFlavor flavor, DataSource dataSource) throws UnsupportedFlavorException, IOException {
		if (XMLStreamReader.class.equals(flavor.getRepresentationClass())) {
			try {
				return XMLInputFactory.newInstance().createXMLStreamReader(dataSource.getInputStream());
			} catch (Exception e) {
				throw new IOException(e);
			}
		} else if (Document.class.equals(flavor.getRepresentationClass())) {
			try {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				factory.setNamespaceAware(true);
				DocumentBuilder parser = factory.newDocumentBuilder();
				return parser.parse(dataSource.getInputStream());
			} catch (Exception e) {
				throw new IOException(e);
			}
		} else if (StreamSource.class.equals(flavor.getRepresentationClass())) {
			try {
				return new StreamSource(dataSource.getInputStream());
			} catch (Exception e) {
				throw new IOException(e);
			}
		}
		throw new IOException(String.format("Unsupported dataflavor %s", flavor));
	}

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[] { STREAM_FLAVOR, DOM_FLAVOR,XSL_SOURCE_FLAVOR };
	}

	@Override
	public byte[] toContent(Object content, String contentType) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		if (content instanceof Document) {
			try {
				TransformerFactory xformFactory = TransformerFactory.newInstance();
				Transformer transformer = xformFactory.newTransformer();
				transformer.transform(new DOMSource((Document) content), new StreamResult(bos));
				return bos.toByteArray();
			} catch (Exception e) {
				throw new IOException(e);
			}
		} else if (content instanceof XMLStreamReader) {
			XMLStreamReader reader = (XMLStreamReader) content;
			// TODO if reader is at start serialize it
			throw new IOException("Unable to serialize XMLStreamReader");
		} else if (content instanceof StreamSource) {
			XMLStreamReader reader = (XMLStreamReader) content;
			// TODO if reader is at start serialize it
			throw new IOException("Unable to serialize StreamSource");
		}
		throw new IOException(String.format("Unsupported object class %s", content.getClass()));
	}

	@Override
	public QName getDefaultQName(DataSource dataSource) {
		QName defaultName = null;
		try {
			InputStream is = dataSource.getInputStream();
			XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(is);
			reader.nextTag();
			String tns = reader.getNamespaceURI();
			reader.close();
			if (tns != null) {
				defaultName = QName.valueOf("{"+tns+"}");
			}
			return defaultName;
		} catch (Exception e) {
			return null;
		}
	}

}
