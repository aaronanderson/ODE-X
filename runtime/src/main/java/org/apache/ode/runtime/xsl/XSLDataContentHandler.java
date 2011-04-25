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
package org.apache.ode.runtime.xsl;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.activation.ActivationDataFlavor;
import javax.activation.DataSource;
import javax.wsdl.WSDLException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.ode.spi.repo.XMLDataContentHandler;

public class XSLDataContentHandler extends XMLDataContentHandler {

	public static final ActivationDataFlavor XSL_FLAVOR = new ActivationDataFlavor(Transformer.class, "application/xsl; x-java-class=XSL", "XSL");

	@Override
	public Object getContent(DataSource dataSource) throws IOException {
		try {
			TransformerFactory xformFactory = TransformerFactory.newInstance();
			Transformer transformer = xformFactory.newTransformer(new StreamSource(dataSource.getInputStream()));
			return transformer;
		} catch (TransformerConfigurationException te) {
			throw new IOException(te);
		}
	}

	@Override
	public Object getTransferData(DataFlavor flavor, DataSource dataSource) throws UnsupportedFlavorException, IOException {
		if (Transformer.class.equals(flavor.getDefaultRepresentationClass())) {
			try {
				TransformerFactory xformFactory = TransformerFactory.newInstance();
				Transformer transformer = xformFactory.newTransformer(new StreamSource(dataSource.getInputStream()));
				return transformer;
			} catch (TransformerConfigurationException te) {
				throw new IOException(te);
			}
		} else {
			return super.getTransferData(flavor, dataSource);
		}
	}

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[] { XSL_FLAVOR, STREAM_FLAVOR, DOM_FLAVOR, XSL_SOURCE_FLAVOR };
	}

	@Override
	public byte[] toContent(Object content, String contentType) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		if (content instanceof Transformer) {
			throw new IOException("Unable to serialize Transformer");
		} else {
			return super.toContent(content, contentType);
		}
	}

}
