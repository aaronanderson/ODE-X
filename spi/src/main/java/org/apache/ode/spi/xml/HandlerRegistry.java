/*
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
package org.apache.ode.spi.xml;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

public class HandlerRegistry<C> {

	Map<QName, Map<Class<?>, ElementHandler<?, C>>> handlers = new HashMap<QName, Map<Class<?>, ElementHandler<?, C>>>();

	public <M> void register(QName qname, Class<M> clazz, ElementHandler<M, C> handler) {
		Map<Class<?>, ElementHandler<?, C>> entry = handlers.get(qname);
		if (entry == null) {
			entry = new HashMap<Class<?>, ElementHandler<?, C>>();
			handlers.put(qname, entry);
		}
		if (clazz != null) {
			entry.put(clazz, handler);
		} else {
			entry.put(Object.class, handler);
		}
	}

	public <M> void unregister(QName qname, Class<M> clazz) {

	}

	public <M> ElementHandler<M, C> retrieve(QName qname, Class<M> clazz) {
		Map<Class<?>, ElementHandler<?, C>> entry = handlers.get(qname);
		if (entry != null) {
			if (clazz != null) {
				ElementHandler<M, C> handler = (ElementHandler<M, C>) entry.get(clazz);
				if (handler == null) {
					for (Map.Entry<Class<?>, ElementHandler<?, C>> canidates : entry.entrySet()) {
						if (clazz.isAssignableFrom(canidates.getKey())) {
							handler = (ElementHandler<M, C>) canidates.getValue();
							break;
						}
					}
				}
				return handler;
			} else {
				return (ElementHandler<M, C>) entry.values().iterator().next();
			}
		}
		return null;
	}

}
