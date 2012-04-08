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
package org.apache.ode.runtime.exec;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.activation.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.ode.runtime.exec.platform.PlatformImpl;
import org.apache.ode.spi.exec.xml.Executable;
import org.apache.ode.spi.repo.JAXBDataContentHandler;
import org.apache.ode.spi.repo.XMLDataContentHandler;

public class ExecDataContentHandler extends XMLDataContentHandler {

	protected PlatformImpl platform;

	public ExecDataContentHandler(PlatformImpl platform) {
		this.platform = platform;
	}

	@Override
	public Object getContent(DataSource dataSource) throws IOException {
		try {
			JAXBContext ctx = platform.getJAXBContext(dataSource.getInputStream());
			Unmarshaller u = ctx.createUnmarshaller();
			return (JAXBElement) u.unmarshal(dataSource.getInputStream());
		} catch (JAXBException je) {
			throw new IOException(je);
		}
	}

	@Override
	public Object getTransferData(DataFlavor flavor, DataSource dataSource) throws UnsupportedFlavorException, IOException {
		if (JAXBElement.class.equals(flavor.getRepresentationClass())) {
			try {
				JAXBContext ctx = platform.getJAXBContext(dataSource.getInputStream());
				Unmarshaller u = ctx.createUnmarshaller();
				return (JAXBElement) u.unmarshal(dataSource.getInputStream());
			} catch (JAXBException je) {
				throw new IOException(je);
			}
		} else {
			return super.getTransferData(flavor, dataSource);
		}
	}

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[] { JAXBDataContentHandler.JAXB_FLAVOR, STREAM_FLAVOR, DOM_FLAVOR, XSL_SOURCE_FLAVOR };
	}

	@Override
	public byte[] toContent(Object content, String contentType) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		if (content instanceof JAXBElement) {
			JAXBElement<Executable> exec = (JAXBElement<Executable>) content;
			try {
				JAXBContext ctx = platform.getJAXBContext(exec.getValue());

				Marshaller u = ctx.createMarshaller();
				u.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
				u.marshal(exec, bos);
				return bos.toByteArray();
			} catch (JAXBException je) {
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
			InputStream is = dataSource.getInputStream();
			XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(is);
			reader.nextTag();
			String tns = reader.getAttributeValue(null, "targetNamespace");
			String name = reader.getAttributeValue(null, "name");
			reader.close();
			if (tns != null && name != null) {
				defaultName = new QName(tns, name);
			}
			return defaultName;
		} catch (Exception e) {
			return null;
		}
	}

}
