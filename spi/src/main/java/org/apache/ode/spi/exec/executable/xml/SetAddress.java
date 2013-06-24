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
package org.apache.ode.spi.exec.executable.xml;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class SetAddress implements SetAdd, SetAddRef {
	
	public SetAddress(String address) {
		this.address = address;
	}

	private String address;

	@Override
	public String address() {
		return address;
	}

	public static class SetAddRefAdapter extends XmlAdapter<String, SetAddRef> {

		@Override
		public String marshal(SetAddRef addr) throws Exception {
			if (addr != null) {
				return addr.address();
			}
			return null;
		}

		@Override
		public SetAddRef unmarshal(String addr) throws Exception {
			return new SetAddress(addr);
		}

	}

	public static class SetAddAdapter extends XmlAdapter<String, SetAdd> {

		@Override
		public String marshal(SetAdd addr) throws Exception {
			if (addr != null) {
				return addr.address();
			}
			return null;
		}

		@Override
		public SetAdd unmarshal(String addr) throws Exception {
			return new SetAddress(addr);
		}

	}

}
