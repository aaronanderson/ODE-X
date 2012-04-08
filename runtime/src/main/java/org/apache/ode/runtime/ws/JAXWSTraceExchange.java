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

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class JAXWSTraceExchange extends JAXWSExchange {

	StringBuilder input = new StringBuilder();
	StringBuilder output = new StringBuilder();
	StringBuilder outHeaders = new StringBuilder();
	OutputStream out;
	InputStream in;

	JAXWSTraceExchange(JAXWSHttpContext context, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		super(context, request, response);
		out = new TraceOutputStream(response.getOutputStream());
		in = new TraceInputStream(request.getInputStream());
		input.append("Request ");
		input.append(super.getRequestURI());
		input.append("\n Headers: \n");
		for (Enumeration e = request.getHeaderNames(); e.hasMoreElements();) {
			String name = (String) e.nextElement();
			input.append("\t Name: ");
			input.append(name);
			input.append(" Value: ");
			input.append(request.getHeader(name));
			input.append("\n");
		}
		input.append("Body: \n");
	}

	@Override
	public void addResponseHeader(String name, String value) {
		outHeaders.append("\t Name: ");
		outHeaders.append(name);
		outHeaders.append(" Value: ");
		outHeaders.append(request.getHeader(name));
		outHeaders.append("\n");
		super.addResponseHeader(name, value);
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("Input: \n");
		buf.append(input);
		buf.append("\nOutput: \n");
		buf.append("Headers: \n");
		buf.append(outHeaders);
		buf.append("\n Body: \n");
		buf.append(output);
		buf.append("\n");
		return buf.toString();
	}

	@Override
	public InputStream getRequestBody() throws IOException {
		return in;
	}

	@Override
	public OutputStream getResponseBody() throws IOException {
		return out;
	}

	@Override
	public Map<String, List<String>> getResponseHeaders() {
		if (responseHeaders == null) {
			responseHeaders = new ResponseTraceHeaderMap();
		}
		return responseHeaders;
	}

	public class TraceOutputStream extends FilterOutputStream {

		public TraceOutputStream(OutputStream out) {
			super(out);
		}

		@Override
		public void write(int b) throws IOException {
			output.append((char) b);
			super.write(b);
		}

		@Override
		public void write(byte[] b) throws IOException {
			for (byte b2 : b) {
				output.append((char) b2);
			}
			super.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			for (int i = off; i < off + len; i++) {
				output.append((char) b[i]);
			}// FilterOut will call the write above causing duplicate
				// output.skip it.
			super.out.write(b, off, len);
		}

	}

	public class TraceInputStream extends FilterInputStream {

		protected TraceInputStream(InputStream in) {
			super(in);
		}

		@Override
		public int read() throws IOException {
			int val = super.read();
			input.append((char) val);
			return val;
		}

		@Override
		public int read(byte[] b) throws IOException {
			int len = super.read(b);
			if (len != -1) {
				for (int i = 0; i < len; i++) {
					input.append((char) b[i]);
				}
			}
			return len;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			int cnt = super.read(b, off, len);
			if (cnt != -1) {
				for (int i = off; i < cnt; i++) {
					input.append((char) b[i]);
				}
			}
			return cnt;
		}

	}

	public class ResponseTraceHeaderMap extends ResponseHeaderMap {
		@Override
		public List<String> put(String name, List<String> values) {
			for (String value : values) {
				outHeaders.append("\t Name: ");
				outHeaders.append(name);
				outHeaders.append(" Value: ");
				outHeaders.append(value);
				outHeaders.append("\n");
			}
			return super.put(name, values);
		}
	}

}
