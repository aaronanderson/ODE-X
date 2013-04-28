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
package org.apache.ode.spi.compiler;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.ode.spi.xml.AttributeHandler;
import org.apache.ode.spi.xml.ElementHandler;
import org.apache.ode.spi.xml.HandlerException;
import org.apache.ode.spi.xml.HandlerRegistry;

public abstract class AbstractHandlerRegistry<M, X extends HandlerException, C, E extends ElementHandler<? extends M, C, X>, A extends AttributeHandler<? extends M, C, X>>
		implements HandlerRegistry<M, C, X, E, A> {

	Map<QName, Map<Class, ElementHandler>> elHandlers = new HashMap<QName, Map<Class, ElementHandler>>();
	Map<QName, Map<QName, Map<Class, AttributeHandler>>> attHandlers = new HashMap<QName, Map<QName, Map<Class, AttributeHandler>>>();

	// @SuppressWarnings("all")
	@Override
	public void register(E handler, QName... ename) throws X {
		// TODO this assumes the handler directly implements the interface and declares raw types for the generic parameters.
		Class type = null;
		for (Type t : handler.getClass().getGenericInterfaces()) {
			if (t instanceof ParameterizedType) {
				ParameterizedType pt = (ParameterizedType) t;
				if (pt.getRawType() instanceof Class && ElementHandler.class.isAssignableFrom((Class) pt.getRawType())) {
					if (pt.getActualTypeArguments()[0] instanceof Class) {
						type = (Class) pt.getActualTypeArguments()[0];
					} else if (pt.getActualTypeArguments()[0] instanceof ParameterizedType) {
						type = (Class) ((ParameterizedType) pt.getActualTypeArguments()[0]).getRawType();
					}
					// ParameterizedType ct = (ParameterizedType) pt.getActualTypeArguments()[0];
					// register(qname, new UnitKey((Class) ct.getActualTypeArguments()[0], (Class) ct.getRawType()), handler);
				}
			}
		}
		if (type == null) {
			throw convertException(new HandlerException("Unable to determine ElementHandler model type"));
		}
		for (QName qname : ename) {
			Map<Class, ElementHandler> entry = elHandlers.get(qname);
			if (entry == null) {
				entry = new HashMap<Class, ElementHandler>();
				elHandlers.put(qname, entry);
			}
			entry.put(type, handler);
		}

	}

	@Override
	public void unregister(QName ename, Class<E> type) throws X {

	}

	@Override
	public E retrieve(QName ename, M model) throws X {
		Class type = model.getClass();
		Map<Class, ElementHandler> entry = elHandlers.get(ename);
		if (entry != null) {
			ElementHandler handler = entry.get(type);
			if (handler == null) {
				for (Map.Entry<Class, ElementHandler> canidates : entry.entrySet()) {
					if (canidates.getKey().isAssignableFrom(type)) {
						handler = canidates.getValue();
						break;
					}
				}
			}
			return (E) handler;
		}
		return null;
	}

	@Override
	public void register(A handler, QName aname, QName... ename) throws X {
		Class type = null;
		for (Type t : handler.getClass().getGenericInterfaces()) {
			if (t instanceof ParameterizedType) {
				ParameterizedType pt = (ParameterizedType) t;
				if (pt.getRawType() instanceof Class && AttributeHandler.class.isAssignableFrom((Class) pt.getRawType())) {
					if (pt.getActualTypeArguments()[0] instanceof Class) {
						type = (Class) pt.getActualTypeArguments()[0];
					} else if (pt.getActualTypeArguments()[0] instanceof ParameterizedType) {
						type = (Class) ((ParameterizedType) pt.getActualTypeArguments()[0]).getRawType();
					}
				}
			}
		}
		if (type == null) {
			throw convertException(new HandlerException("Unable to determine ElementHandler model type"));
		}
		for (QName qname : ename) {
			Map<QName, Map<Class, AttributeHandler>> eentry = attHandlers.get(qname);
			if (eentry == null) {
				eentry = new HashMap<QName, Map<Class, AttributeHandler>>();
				attHandlers.put(qname, eentry);
			}
			Map<Class, AttributeHandler> aentry = eentry.get(aname);
			if (aentry == null) {
				aentry = new HashMap<Class, AttributeHandler>();
				eentry.put(aname, aentry);
			}
			aentry.put(type, (AttributeHandler) handler);
		}

	}

	@Override
	public void unregister(QName ename, QName aname, Class<A> type) throws X {

	}

	@Override
	public A retrieve(QName ename, QName aname, M model) throws X {
		Class type = model.getClass();
		Map<QName, Map<Class, AttributeHandler>> eentry = attHandlers.get(ename);
		if (eentry != null) {
			Map<Class, AttributeHandler> aentry = eentry.get(aname);
			if (aentry != null) {
				AttributeHandler handler = aentry.get(type);
				if (handler == null) {
					for (Map.Entry<Class, AttributeHandler> canidates : aentry.entrySet()) {
						if (canidates.getKey().isAssignableFrom(type)) {
							handler = canidates.getValue();
							break;
						}
					}
				}
				return (A) handler;
			}
		}
		return null;
	}

	abstract public X convertException(HandlerException e);

}
