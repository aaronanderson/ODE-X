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
package org.apache.ode.runtime.ws;

import java.util.Set;

import javax.xml.ws.spi.http.HttpContext;
import javax.xml.ws.spi.http.HttpHandler;

import org.apache.ode.runtime.ws.JAXWSServlet.JAXWSAttributeInfo;

public class JAXWSHttpContext extends HttpContext {

	final JAXWSAttributeInfo info;
	final String context;
	final String path;

	JAXWSHttpContext(JAXWSAttributeInfo info, String context, String path) {
		this.info = info;
		this.context = context;
		this.path = path;
	}

	@Override
	public Object getAttribute(String name) {
		return info.getAttribute(name);
	}

	@Override
	public Set<String> getAttributeNames() {
		return info.getAttributeNames();
	}

	@Override
	public String getPath() {
		return path;
	}

	public String getContext() {
		return context;
	}

	public HttpHandler getHandler() {
		return this.handler;
	}

}