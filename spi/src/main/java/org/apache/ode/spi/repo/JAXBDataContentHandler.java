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

import org.apache.ode.spi.exec.xml.Executable;

public class JAXBDataContentHandler extends XMLDataContentHandler {

	public static final ActivationDataFlavor JAXB_FLAVOR = new ActivationDataFlavor(JAXBElement.class, "application/xml; x-java-class=JAXB", "JAXB");

	protected JAXBContext jc;

	public JAXBDataContentHandler(JAXBContext jc) {
		this.jc = jc;
	}

	protected JAXBDataContentHandler() {

	}

	protected JAXBContext getJAXBContext(DataSource dataSource) throws JAXBException {
		return this.jc;
	}

	protected JAXBContext getJAXBContext(JAXBElement<Executable> exec) throws JAXBException {
		return this.jc;
	}

	@Override
	public Object getContent(DataSource dataSource) throws IOException {
		try {
			JAXBContext ctx = getJAXBContext(dataSource);
			if (ctx != null) {

				Unmarshaller u = ctx.createUnmarshaller();
				return (JAXBElement) u.unmarshal(dataSource.getInputStream());
			} else {
				throw new IOException("JAXBContext is null");
			}
		} catch (JAXBException je) {
			throw new IOException(je);
		}
	}

	@Override
	public Object getTransferData(DataFlavor flavor, DataSource dataSource) throws UnsupportedFlavorException, IOException {
		if (JAXBElement.class.equals(flavor.getDefaultRepresentationClass())) {
			try {
				JAXBContext ctx = getJAXBContext(dataSource);
				if (ctx != null) {
					Unmarshaller u = ctx.createUnmarshaller();
					return (JAXBElement) u.unmarshal(dataSource.getInputStream());
				} else {
					throw new IOException("JAXBContext is null");
				}
			} catch (JAXBException je) {
				throw new IOException(je);
			}
		} else {
			return super.getTransferData(flavor, dataSource);
		}
	}

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[] { JAXB_FLAVOR, STREAM_FLAVOR, DOM_FLAVOR, XSL_SOURCE_FLAVOR };
	}

	@Override
	public byte[] toContent(Object content, String contentType) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		if (content instanceof JAXBElement) {
			JAXBElement<Executable> exec = (JAXBElement<Executable>) content;
			try {
				JAXBContext ctx = getJAXBContext(exec);
				if (ctx != null) {

					Marshaller u = ctx.createMarshaller();
					u.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
					u.marshal(exec, bos);
					return bos.toByteArray();
				} else {
					throw new IOException("JAXBContext is null");
				}
			} catch (JAXBException je) {
				throw new IOException(je);
			}
		} else {
			return super.toContent(content, contentType);
		}
	}

}
