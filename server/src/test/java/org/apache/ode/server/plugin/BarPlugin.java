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
package org.apache.ode.server.plugin;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.activation.ActivationDataFlavor;
import javax.activation.CommandObject;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.MimeTypeParseException;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.ode.spi.Plugin;
import org.apache.ode.spi.repo.DataContentHandler;
import org.apache.ode.spi.repo.ArtifactDataSource;
import org.apache.ode.spi.repo.Repository;

@Singleton
@Named("BarPlugin")
public class BarPlugin implements Plugin {

	public static String BAR_MIMETYPE = "application/bar";
	// @Inject WSDLPlugin wsdlPlugin;
	@Inject
	Repository repository;
	@Inject
	Provider<BarValidation> barProvider;
	@Inject
	Provider<ArtifactDataSource> dsProvider;

	@PostConstruct
	public void init() {
		System.out.println("Initializing BPELPlugin");
		repository.registerExtension("bar", BAR_MIMETYPE);
		repository.registerExtension("bar2", BAR_MIMETYPE);
		repository.registerCommandInfo(BAR_MIMETYPE, "validate", true, barProvider);
		repository.registerHandler(BAR_MIMETYPE, new BarDataContentHandler());
		System.out.println("BPELPlugin Initialized");

	}

	public ArtifactDataSource getArtifactDataSource(String fileName) throws MimeTypeParseException {
		ArtifactDataSource ds = dsProvider.get();
		ds.configure(new byte[0], fileName);
		return ds;
	}

	public DataHandler getDataHandler(File file) throws MimeTypeParseException, IOException {
		ArtifactDataSource ds = dsProvider.get();
		byte[] contents = DataContentHandler.readStream(new FileInputStream(file));
		/*
		 * FileInputStream fis = new FileInputStream(file); byte[] buffer = new
		 * byte[4096]; int index = 0; while (index > -1){ fis.read(b); }
		 * fis.getChannel().;
		 */
		ds.configure(contents, file.getName());
		return repository.getDataHandler(ds);
	}

	public Repository getRepository() {
		return repository;
	}

	static public class BarValidation implements CommandObject {

		DataHandler handler;

		@Override
		public void setCommandContext(String command, DataHandler handler) throws IOException {
			this.handler = handler;
		}

		public boolean isValid() throws Exception {

			Bar bar = (Bar) handler.getContent();
			return "Foo".equals(bar.getPayload());
		}
	}

	static public class BarDataContentHandler extends DataContentHandler {

		@Override
		public Object getContent(DataSource ds) throws IOException {
			byte[] payload = DataContentHandler.read(ds);
			return new Bar(new String(payload));
		}

		@Override
		public Object getTransferData(DataFlavor df, DataSource ds) throws UnsupportedFlavorException, IOException {
			if (Bar.class.isAssignableFrom(df.getRepresentationClass())) {
				return getContent(ds);
			}
			return null;
		}

		@Override
		public DataFlavor[] getTransferDataFlavors() {
			DataFlavor[] flavors = new DataFlavor[1];
			flavors[0] = new ActivationDataFlavor(Bar.class, BAR_MIMETYPE + "; x-java-class=Bar", "Bar Representation");
			return flavors;
		}

		@Override
		public byte[] toContent(Object content, String mimeType) throws IOException {
			if (content instanceof Bar) {
				return ((Bar) content).getPayload().getBytes();
			}
			throw new IOException(String.format("%s is not an instance of Bar", content.getClass()));
		}

	}

	static public class Bar {
		private String payload;

		public Bar() {
		}

		public Bar(String payload) {
			this.payload = payload;
		}

		public void setPayload(String payload) {
			this.payload = payload;
		}

		public String getPayload() {
			return payload;
		}
	}


}
