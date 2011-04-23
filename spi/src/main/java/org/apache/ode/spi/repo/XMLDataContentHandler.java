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

import javax.activation.ActivationDataFlavor;
import javax.activation.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

public class XMLDataContentHandler extends DataContentHandler {
	JAXBContext jc;

	public XMLDataContentHandler(JAXBContext jc) {
		this.jc = jc;
	}

	@Override
	public Object getContent(DataSource dataSource) throws IOException {
		if (jc != null) {
			try {
				Unmarshaller u = jc.createUnmarshaller();
				return (JAXBElement) u.unmarshal(dataSource.getInputStream());
			} catch (JAXBException je) {
				throw new IOException(je);
			}
		} else {
			try {
				return XMLInputFactory.newInstance().createXMLStreamReader(dataSource.getInputStream());
			} catch (Exception e) {
				throw new IOException(e);
			}
		}
	}

	@Override
	public Object getTransferData(DataFlavor flavor, DataSource dataSource) throws UnsupportedFlavorException, IOException {
		if (JAXBElement.class.equals(flavor.getDefaultRepresentationClass())) {
			try {
				Unmarshaller u = jc.createUnmarshaller();
				return (JAXBElement) u.unmarshal(dataSource.getInputStream());
			} catch (JAXBException je) {
				throw new IOException(je);
			}
		} else if (XMLStreamReader.class.equals(flavor.getDefaultRepresentationClass())) {
			try {
				return XMLInputFactory.newInstance().createXMLStreamReader(dataSource.getInputStream());
			} catch (Exception e) {
				throw new IOException(e);
			}
		} else if (Document.class.equals(flavor.getDefaultRepresentationClass())) {
			try {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				factory.setNamespaceAware(true);
				DocumentBuilder parser = factory.newDocumentBuilder();
				return parser.parse(dataSource.getInputStream());
			} catch (Exception e) {
				throw new IOException(e);
			}
		}
		throw new IOException(String.format("Unsupported dataflavor %s", flavor));
	}

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		ActivationDataFlavor xsd = new ActivationDataFlavor(XMLStreamReader.class, "application/xml; x-java-class=XMLStreamReader", "Stax XML Stream Reader");
		ActivationDataFlavor dd = new ActivationDataFlavor(Document.class, "application/xml; x-java-class=W3CDom", "W3C DOM");
		if (jc != null) {
			ActivationDataFlavor jd = new ActivationDataFlavor(JAXBElement.class, "application/xml; x-java-class=JAXB", "JAXB");
			return new DataFlavor[] { jd, xsd, dd };
		} else {
			return new DataFlavor[] { xsd, dd };
		}
	}

	@Override
	public byte[] toContent(Object content, String contentType) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		if (content instanceof JAXBElement) {
			try {
				Marshaller u = jc.createMarshaller();
				u.marshal((JAXBElement) content, bos);
				return bos.toByteArray();
			} catch (JAXBException je) {
				throw new IOException(je);
			}
		} else if (content instanceof Document) {
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
		}
		throw new IOException(String.format("Unsupported object class %s", content.getClass()));
	}

}
