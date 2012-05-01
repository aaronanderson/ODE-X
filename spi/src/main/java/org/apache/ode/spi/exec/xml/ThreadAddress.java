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
package org.apache.ode.spi.exec.xml;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class ThreadAddress implements ThdAdd, ThdAddRef {
	public ThreadAddress() {
	}

	public ThreadAddress(String address) {
		this.address = address;
	}

	private String address;

	@Override
	public String address() {
		return address;
	}

	public static class ThdAddRefAdapter extends XmlAdapter<String, ThdAddRef> {

		@Override
		public String marshal(ThdAddRef addr) throws Exception {
			if (addr != null) {
				return addr.address();
			}
			return null;
		}

		@Override
		public ThdAddRef unmarshal(String addr) throws Exception {
			return new ThreadAddress(addr);
		}

	}

	public static class ThdAddAdapter extends XmlAdapter<String, ThdAdd> {

		@Override
		public String marshal(ThdAdd addr) throws Exception {
			if (addr != null) {
				return addr.address();
			}
			return null;
		}

		@Override
		public ThdAdd unmarshal(String addr) throws Exception {
			ThreadAddress i = new ThreadAddress(addr);
			return i;
		}

	}

}
