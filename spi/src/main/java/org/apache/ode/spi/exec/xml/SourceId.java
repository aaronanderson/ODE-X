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

public class SourceId implements SrcId, SrcIdRef {
	public SourceId() {
	}

	public SourceId(String id) {
		this.id = id;
	}

	private String id;

	@Override
	public String id() {
		return id;
	}

	public static class SrcIdAdapter extends XmlAdapter<String, SrcId> {

		@Override
		public String marshal(SrcId id) throws Exception {
			if (id != null) {
				return id.id();
			}
			return null;
		}

		@Override
		public SrcId unmarshal(String id) throws Exception {
			return new SourceId(id);
		}

	}

	public static class SrcIdRefAdapter extends XmlAdapter<String, SrcIdRef> {

		@Override
		public String marshal(SrcIdRef id) throws Exception {
			if (id != null) {
				return id.id();
			}
			return null;
		}

		@Override
		public SrcIdRef unmarshal(String id) throws Exception {
			return new SourceId(id);
		}

	}

}
