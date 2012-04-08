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

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class JAXWSServlet extends HttpServlet implements JAXWSHandler {

	private Map<String, Map<String, JAXWSHttpContext>> ctx = new ConcurrentHashMap<String, Map<String, JAXWSHttpContext>>();

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String target = request.getRequestURI();
		for (Map.Entry<String, Map<String, JAXWSHttpContext>> centry : ctx.entrySet()) {
			if (target.startsWith(centry.getKey())) {
				for (Map.Entry<String, JAXWSHttpContext> pentry : centry.getValue().entrySet()) {
					if (target.substring(centry.getKey().length()).equals(pentry.getKey())) {
						// TODO add logging support and enable trace based on
						// log level
						// JAXWSExchange exchange = new
						// JAXWSExchange(pentry.getValue(), request, response);
						JAXWSTraceExchange exchange = new JAXWSTraceExchange(pentry.getValue(), request, response);
						pentry.getValue().getHandler().handle(exchange);
						// System.out.println(exchange);
						return;
					}
				}
			}
		}
		response.setStatus(HttpServletResponse.SC_NOT_FOUND);
	}

	public JAXWSHttpContext register(String context, String path) throws Exception {
		if (context == null || !context.startsWith("/")) {
			throw new Exception("context must not be null and begin with /");
		}
		if (path == null || !path.startsWith("/")) {
			throw new Exception("path must not be null and begin with /");
		}
		Map<String, JAXWSHttpContext> entry = ctx.get(context);
		if (entry == null) {
			entry = new ConcurrentHashMap<String, JAXWSHttpContext>();
			ctx.put(context, entry);
		} else {
			if (entry.containsKey(path)) {
				throw new Exception(String.format("Context %s path %s already registered", context, path));
			}
		}
		JAXWSHttpContext hctx = new JAXWSHttpContext(new JAXWSAttributeInfo(), context, path);
		entry.put(path, hctx);
		return hctx;
	}

	public Set<JAXWSHttpContext> list(String context) {
		HashSet<JAXWSHttpContext> set = new HashSet<JAXWSHttpContext>();
		if (context == null) {
			for (Map<String, JAXWSHttpContext> entry : ctx.values()) {
				for (JAXWSHttpContext jctx : entry.values()) {
					set.add(jctx);
				}
			}
			return set;
		}

		Map<String, JAXWSHttpContext> entry = ctx.get(context);
		if (entry != null) {
			set.addAll(entry.values());
		}
		return set;
	}

	public void unregister(JAXWSHttpContext context) throws Exception {
		Map<String, JAXWSHttpContext> entry = ctx.get(context.getContext());
		if (entry != null) {
			entry.remove(context.getPath());
		}

	}

	public class JAXWSAttributeInfo {

		public Object getAttribute(String name) {
			return getServletContext().getAttribute(name);
		}

		public Set<String> getAttributeNames() {
			Set<String> attrNames = new HashSet<String>();
			for (Iterator<String> i = (Iterator<String>) getServletContext().getAttributeNames(); i.hasNext();) {
				attrNames.add(i.next());
			}
			return attrNames;
		}

	}

}
