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

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.namespace.QName;

public class DestinationChannelAddress extends ChannelAddress implements DestinationChannelAdd {
	
	public DestinationChannelAddress(String address, QName type) {
		super(address, type);
	}
	
	public DestinationChannelAddress(String address, QName type, Class<?> javaType) {
		super(address, type, javaType);
	}

	

	public static class DestinationChannelAddAdapter  extends XmlAdapter<String, DestinationChannelAdd> {

		@Override
		public String marshal(DestinationChannelAdd addr) throws Exception {
			if (addr != null) {
				return addr.address();
			}
			return null;
		}

		@Override
		public DestinationChannelAdd unmarshal(String addr) throws Exception {
			return new DestinationChannelAddress(addr,null);
		}

	}

}
