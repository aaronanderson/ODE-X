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

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class BondValue implements BondId, BondRef {
	public BondValue() {
	}

	public BondValue(String id) {
		this.id = id;
	}

	private String id;

	@Override
	public String id() {
		return id;
	}

	public static class BondRefAdapter extends XmlAdapter<String, BondRef> {

		@Override
		public String marshal(BondRef id) throws Exception {
			if (id != null) {
				return id.id();
			}
			return null;
		}

		@Override
		public BondRef unmarshal(String id) throws Exception {
			return new BondValue(id);
		}

	}

	public static class BondIdAdapter extends XmlAdapter<String, BondId> {

		@Override
		public String marshal(BondId id) throws Exception {
			if (id != null) {
				return id.id();
			}
			return null;
		}

		@Override
		public BondId unmarshal(String id) throws Exception {
			BondValue i = new BondValue(id);
			return i;
		}

	}

}