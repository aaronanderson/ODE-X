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

import java.io.ByteArrayInputStream;
import java.util.logging.Logger;

import org.apache.ode.runtime.xsd.XSDContextImpl;
import org.xml.sax.InputSource;

public class WSDLLocatorImpl implements javax.wsdl.xml.WSDLLocator {
	byte[] contents;
	String lastResolved = null;

	private static final Logger log = Logger.getLogger(WSDLLocatorImpl.class.getName());

	public WSDLLocatorImpl(byte[] contents, WSDLContextImpl wsdls, XSDContextImpl xsds) {
		this.contents = contents;
	}

	@Override
	public void close() {

	}

	@Override
	public InputSource getBaseInputSource() {
		log.info("getBaseInputSource");
		return new InputSource(new ByteArrayInputStream(contents));
	}

	@Override
	public String getBaseURI() {
		log.info("getBaseURI");
		return null;
	}

	@Override
	public InputSource getImportInputSource(String parentLocation, String importLocation) {
		/*try {
			//InputSource source = new InputSource(ds.getInputStream());
			//lastResolved = importLocation;
			//return source;
		} catch (IOException e) {
			log.log(Level.SEVERE,"",e);
			return null;
		}*/
		log.info("getImportInputSource");
		return null;

	}

	@Override
	public String getLatestImportURI() {
		log.info("getLatestImportURI");
		return lastResolved;
	}

}
