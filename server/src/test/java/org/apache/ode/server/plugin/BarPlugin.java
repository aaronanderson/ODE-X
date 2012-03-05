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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Logger;

import javax.activation.ActivationDataFlavor;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.MimeTypeParseException;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.ode.spi.repo.ArtifactDataSource;
import org.apache.ode.spi.repo.DataContentHandler;
import org.apache.ode.spi.repo.Repository;
import org.apache.ode.spi.repo.Validate;

@Singleton
@Named("BarPlugin")
public class BarPlugin {

	public static final String BAR_MIMETYPE = "application/bar";
	public static final String FOO_MIMETYPE = "application/foo";
	// @Inject WSDLPlugin wsdlPlugin;
	@Inject
	Repository repository;
	@Inject
	Provider<BarValidation> barProvider;
	@Inject
	Provider<ArtifactDataSource> dsProvider;

	private static final Logger log = Logger.getLogger(BarPlugin.class.getName());

	@PostConstruct
	public void init() {
		log.fine("Initializing BARPlugin");
		repository.registerFileExtension("bar", BAR_MIMETYPE);
		repository.registerFileExtension("bar2", BAR_MIMETYPE);
		repository.registerNamespace("http://foo", FOO_MIMETYPE);
		repository.registerCommandInfo(BAR_MIMETYPE, Validate.VALIDATE_CMD, true, barProvider);
		repository.registerHandler(BAR_MIMETYPE, new BarDataContentHandler());
		log.fine("BARPlugin Initialized");

	}

	public ArtifactDataSource getArtifactDataSource(String fileName) throws MimeTypeParseException {
		ArtifactDataSource ds = dsProvider.get();
		ds.configure(new byte[0], fileName);
		return ds;
	}

	public DataHandler getDataHandlerByFilename(File file) throws MimeTypeParseException, IOException {
		ArtifactDataSource ds = dsProvider.get();
		byte[] contents = DataContentHandler.readStream(new FileInputStream(file));
		ds.configure(contents, file.getName());
		return repository.getDataHandler(ds);
	}

	public DataHandler getDataHandlerByContent(File file) throws MimeTypeParseException, IOException {
		ArtifactDataSource ds = dsProvider.get();
		byte[] contents = DataContentHandler.readStream(new FileInputStream(file));
		ds.configure(contents);
		return repository.getDataHandler(ds);
	}

	public Repository getRepository() {
		return repository;
	}

	static public class BarValidation implements Validate {

		DataHandler handler;

		@Override
		public void setCommandContext(String command, DataHandler handler) throws IOException {
			this.handler = handler;
		}

		@Override
		public boolean validate(StringBuilder sb) {
			try {
				Bar bar = (Bar) handler.getContent();
				return "Foo".equals(bar.getPayload());
			} catch (Exception e) {
				return false;
			}
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
