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
package org.apache.ode.bpel.repo;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import javax.activation.ActivationDataFlavor;
import javax.activation.DataSource;

import org.apache.ode.spi.repo.DataContentHandler;
import org.apache.ode.spi.repo.XMLDataContentHandler;

import static org.apache.ode.bpel.plugin.BPELPlugin.BPEL_MIMETYPE;;

public class BPELContentHandler extends XMLDataContentHandler{

	@Override
	public Object getContent(DataSource ds) throws IOException {
		byte[] payload = DataContentHandler.read(ds);
		//return new Bar(new String(payload));
		return new Object();
	}

	@Override
	public Object getTransferData(DataFlavor df, DataSource ds) throws UnsupportedFlavorException, IOException {
		if (Object.class.isAssignableFrom(df.getRepresentationClass())) {
			return getContent(ds);
		}
		return null;
	}

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		DataFlavor[] flavors = new DataFlavor[1];
		flavors[0] = new ActivationDataFlavor(Object.class, BPEL_MIMETYPE + "; x-java-class=BPEL", "BPEL Representation");
		return flavors;
	}

	@Override
	public byte[] toContent(Object content, String mimeType) throws IOException {
		//if (content instanceof Bar) {
		//	return ((Bar) content).getPayload().getBytes();
		//}
		
		throw new IOException(String.format("%s is not an instance of BPEL", content.getClass()));
	}

}
