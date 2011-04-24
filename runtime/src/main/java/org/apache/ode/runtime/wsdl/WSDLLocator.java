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

import java.io.IOException;

import javax.activation.DataSource;

import org.apache.ode.spi.repo.DependentArtifactDataSource;
import org.xml.sax.InputSource;

public class WSDLLocator implements javax.wsdl.xml.WSDLLocator {
	DependentArtifactDataSource dataSource;
	String lastResolved = null;

	public WSDLLocator(DependentArtifactDataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public void close() {

	}

	@Override
	public InputSource getBaseInputSource() {
		try {
			return new InputSource(dataSource.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public String getBaseURI() {
		return null;
	}

	@Override
	public InputSource getImportInputSource(String parentLocation, String importLocation) {
		DataSource ds = dataSource.getDependency(importLocation);
		if (ds != null) {
			try {
				InputSource source = new InputSource(ds.getInputStream());
				lastResolved = importLocation;
				return source;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		return null;

	}

	@Override
	public String getLatestImportURI() {
		return lastResolved;
	}

}
