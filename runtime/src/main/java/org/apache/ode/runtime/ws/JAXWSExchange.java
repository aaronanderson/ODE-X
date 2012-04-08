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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.spi.http.HttpContext;
import javax.xml.ws.spi.http.HttpExchange;

public class JAXWSExchange extends HttpExchange {

final HttpServletRequest request;
final HttpServletResponse response;
final JAXWSHttpContext context;

Set<String> attrNames;
Map<String, List<String>> requestHeaders;
Map<String, List<String>> responseHeaders;

JAXWSExchange(JAXWSHttpContext context, HttpServletRequest request, HttpServletResponse response) {
this.context = context;
this.request = request;
this.response = response;
}

@Override
public void addResponseHeader(String name, String value) {
response.setHeader(name, value);

}

@Override
public void close() throws IOException {
response.getOutputStream().close();
}

@Override
public Object getAttribute(String name) {
return request.getAttribute(name);
}

@Override
public Set<String> getAttributeNames() {
if (attrNames == null) {
attrNames = new HashSet<String>();
for (Enumeration e = request.getAttributeNames(); e.hasMoreElements();) {
attrNames.add((String) e.nextElement());
}
}
return attrNames;
}

@Override
public String getContextPath() {
return context.getContext();
}

@Override
public HttpContext getHttpContext() {
return context;
}

@Override
public InetSocketAddress getLocalAddress() {
return new InetSocketAddress(request.getLocalAddr(), request.getLocalPort());
}

@Override
public String getPathInfo() {
return request.getPathInfo();
}

@Override
public String getProtocol() {
return request.getProtocol();
}

@Override
public String getQueryString() {
return request.getQueryString();
}

@Override
public InetSocketAddress getRemoteAddress() {
return new InetSocketAddress(request.getRemoteAddr(), request.getRemotePort());
}

@Override
public InputStream getRequestBody() throws IOException {
return request.getInputStream();
}

@Override
public String getRequestHeader(String name) {
return request.getHeader(name);
}

@Override
public Map<String, List<String>> getRequestHeaders() {
if (requestHeaders == null) {
requestHeaders = new HashMap<String, List<String>>();
Enumeration<?> en = request.getHeaderNames();
while (en.hasMoreElements()) {
String name = (String) en.nextElement();
List<String> values = new ArrayList<String>();
requestHeaders.put(name, values);
Enumeration<?> en2 = request.getHeaders(name);
while (en2.hasMoreElements()) {
String value = (String) en2.nextElement();
values.add(value);
}
}
}
return requestHeaders;
}

@Override
public String getRequestMethod() {
return request.getMethod();
}

@Override
public String getRequestURI() {
return request.getRequestURI();
}

@Override
public OutputStream getResponseBody() throws IOException {
return response.getOutputStream();
}

@Override
public Map<String, List<String>> getResponseHeaders() {
if (responseHeaders == null) {
responseHeaders = new ResponseHeaderMap();
}
return responseHeaders;
}

@Override
public String getScheme() {
return request.getScheme();
}

@Override
public Principal getUserPrincipal() {
return request.getUserPrincipal();
}

@Override
public boolean isUserInRole(String role) {
if (request.getUserPrincipal() != null) {
// return request.getUserPrincipal().isUserInRole(role);
}
return false;
}

@Override
public void setStatus(int status) {
response.setStatus(status);
}

public class ResponseHeaderMap extends HashMap<String, List<String>> {
@Override
public List<String> put(String name, List<String> values) {
for (String value : values) {
response.addHeader(name, value);
}
return super.put(name, values);
}
}

}