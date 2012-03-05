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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;

import javax.activation.CommandMap;
import javax.activation.DataSource;
import javax.xml.namespace.QName;

public class DataHandler extends javax.activation.DataHandler {
	private DataSource ds;
	private Object content;
	private String mimeType;
	private CommandMap commandMap;

	public DataHandler(DataSource ds) {
		super(ds);
		this.ds = ds;
	}

	public DataHandler(Object obj, String mimeType) {
		super(obj, mimeType);
		this.content = obj;
		this.mimeType = mimeType;
	}

	public DataHandler(URL url) {
		super(url);
	}

	public synchronized void setCommandMap(CommandMap commandMap) {
		this.commandMap = commandMap;
		super.setCommandMap(commandMap);

	}

	public byte[] toContent() throws IOException {
		if (this.ds != null) {
			if (ds instanceof ArtifactDataSource) {
				return ((ArtifactDataSource) ds).getContent();
			} else {
				DataContentHandler.readStream(ds.getInputStream());
			}
		} else {
			javax.activation.DataContentHandler dch = commandMap.createDataContentHandler(mimeType);
			if (dch instanceof DataContentHandler) {
				return ((DataContentHandler) dch).toContent(content, mimeType);
			} else {
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				super.writeTo(bos);
				return bos.toByteArray();
			}
		}
		throw new IOException("Unable to obtain content");
	}

	public QName getDefaultQName() {
		javax.activation.DataContentHandler dch = commandMap.createDataContentHandler(getContentType());
		if (dch instanceof DataContentHandler) {
			return ((DataContentHandler) dch).getDefaultQName(getDataSource());
		}
		return null;
	}

}
