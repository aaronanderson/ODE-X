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

import javax.activation.ActivationDataFlavor;
import javax.activation.DataSource;
import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.bind.JAXBException;

import org.apache.ode.spi.repo.DependentArtifactDataSource;
import org.apache.ode.spi.repo.XMLDataContentHandler;

public class WSDLDataContentHandler extends XMLDataContentHandler {

	public static final ActivationDataFlavor WSDL_FLAVOR = new ActivationDataFlavor(Definition.class, "application/wsdl; x-java-class=WSDL4J", "WSDL");

	@Override
	public Object getContent(DataSource dataSource) throws IOException {
		if (dataSource instanceof DependentArtifactDataSource){
			try {
				WSDLFactory factory = WSDLFactory.newInstance();
				WSDLReader reader = factory.newWSDLReader();
				return reader.readWSDL(new WSDLLocator((DependentArtifactDataSource)dataSource));
			} catch (WSDLException je) {
				throw new IOException(je);
			}
		}
		throw new IOException("Unable to process WSDL without dependencies");
		
	}

	@Override
	public Object getTransferData(DataFlavor flavor, DataSource dataSource) throws UnsupportedFlavorException, IOException {
		if (Definition.class.equals(flavor.getDefaultRepresentationClass())) {
			if (!(dataSource instanceof DependentArtifactDataSource)){
				throw new IOException("Unable to process WSDL without dependencies");
			}
			try {
				WSDLFactory factory = WSDLFactory.newInstance();
				WSDLReader reader = factory.newWSDLReader();
				return reader.readWSDL(new WSDLLocator((DependentArtifactDataSource)dataSource));
			} catch (WSDLException je) {
				throw new IOException(je);
			}
		} else {
			return super.getTransferData(flavor, dataSource);
		}
	}

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[] { WSDL_FLAVOR, STREAM_FLAVOR, DOM_FLAVOR };
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

}
