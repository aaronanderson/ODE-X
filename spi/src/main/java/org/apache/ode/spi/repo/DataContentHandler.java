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
package org.apache.ode.spi.repo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;
import javax.xml.namespace.QName;

/**
 * 
 * Since JPA limits BLOBs to byte array we will extend the activation classes to
 * do IO against byte arrays to limit needless stream conversions
 * 
 */
public abstract class DataContentHandler implements javax.activation.DataContentHandler {

	@Override
	public void writeTo(Object content, String contentType, OutputStream os) throws IOException {
		byte[] data = toContent(content, contentType);
		os.write(data, 0, data.length);

	}
	
	public QName getDefaultQName(DataSource dataSource) {
		return null;
	}

	public abstract byte[] toContent(Object content, String contentType) throws IOException;

	public static byte[] read(DataSource ds) throws IOException {
		if (ds instanceof ArtifactDataSource) {
			return ((ArtifactDataSource) ds).getContent();
		}

		return readStream(ds.getInputStream());
	}

	public static byte[] readStream(InputStream is) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] buff = new byte[8192];
		int len = is.read(buff);
		while (len > -1) {
			bos.write(buff, 0, len);
			len = is.read(buff);
		}
		is.close();
		bos.close();
		return bos.toByteArray();
	}

}
