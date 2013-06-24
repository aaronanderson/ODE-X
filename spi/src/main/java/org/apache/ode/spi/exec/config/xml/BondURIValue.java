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
package org.apache.ode.spi.exec.config.xml;

import java.net.URI;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class BondURIValue implements BondURI {

	public BondURIValue() {
	}

	public BondURIValue(URI uri) {
		this.uri = uri;
	}

	private URI uri;

	@Override
	public URI uri() {
		return uri;
	}

	public static class BondURIAdapter extends XmlAdapter<String, BondURI> {

		@Override
		public String marshal(BondURI uri) throws Exception {
			if (uri != null) {
				return uri.uri().toString();
			}
			return null;
		}

		@Override
		public BondURI unmarshal(String uri) throws Exception {
			return new BondURIValue(new URI(uri));
		}

	}

}
