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

public class BlockAddress implements BlcAdd, BlcAddRef {
	
	public BlockAddress(String address) {
		this.address = address;
	}

	private String address;

	@Override
	public String address() {
		return address;
	}

	public static class BlcAddRefAdapter extends XmlAdapter<String, BlcAddRef> {

		@Override
		public String marshal(BlcAddRef addr) throws Exception {
			if (addr != null) {
				return addr.address();
			}
			return null;
		}

		@Override
		public BlcAddRef unmarshal(String addr) throws Exception {
			return new BlockAddress(addr);
		}

	}

	public static class BlcAddAdapter extends XmlAdapter<String, BlcAdd> {

		@Override
		public String marshal(BlcAdd addr) throws Exception {
			if (addr != null) {
				return addr.address();
			}
			return null;
		}

		@Override
		public BlcAdd unmarshal(String addr) throws Exception {
			return new BlockAddress(addr);
		}

	}

}
