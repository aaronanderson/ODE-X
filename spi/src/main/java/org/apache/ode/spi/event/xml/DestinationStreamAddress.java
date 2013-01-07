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
package org.apache.ode.spi.event.xml;

import javax.xml.namespace.QName;

public class DestinationStreamAddress<E> extends DestinationChannelAddress<E> implements DestinationStreamAdd<E> {

	public DestinationStreamAddress(String address, QName type) {
		super(address, type);
	}

	public DestinationStreamAddress(String address, QName type, Class<E> javaType) {
		super(address, type, javaType);
	}

	public static class DestinationStreamAddAdapter<E> extends ChannelAddAdapter<DestinationStreamAdd<E>> {

		@Override
		public String marshal(DestinationStreamAdd<E> addr) throws Exception {
			if (addr != null) {
				return addr.address();
			}
			return null;
		}

		@Override
		public DestinationStreamAdd<E> unmarshal(String addr) throws Exception {
			return new DestinationStreamAddress(addr, null);
		}

	}

}
