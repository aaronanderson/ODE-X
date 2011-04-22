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
import java.io.IOException;

import javax.activation.ActivationDataFlavor;
import javax.activation.DataSource;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Document;

public class XMLDataContentHandler extends DataContentHandler {

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
		return null;
	}

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		DataFlavor [] flavors = new DataFlavor[2]; 
		flavors[0] = new ActivationDataFlavor(XMLStreamReader.class, "application/xml; x-java-class=XMLStreamReader","Stax XML Stream Reader");
		flavors[1] = new ActivationDataFlavor(Document.class, "application/xml; x-java-class=W3CDom","W3C DOM");
		return flavors;
	}

	@Override
	public byte[] toContent(Object content, String contentType) throws IOException {
		if (content instanceof XMLStreamReader){
			XMLStreamReader reader = (XMLStreamReader)content;
			//TODO if reader is at start serialize it
			throw new IOException("Unable to serialize XMLStreamReader");
		} else if (content instanceof Document){
			
		}
		return null;
	}

	

}
