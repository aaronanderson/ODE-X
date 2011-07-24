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
package org.apache.ode.runtime.build;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.ode.spi.compiler.AttributeParser;
import org.apache.ode.spi.compiler.ElementParser;
import org.apache.ode.spi.compiler.ParserRegistry;
import org.apache.ode.spi.compiler.Unit;
import org.apache.ode.spi.exec.xml.Instruction;

public class ParserRegistryImpl implements ParserRegistry<org.apache.ode.runtime.build.ParserRegistryImpl.UnitKey> {

	Map<QName, Map<UnitKey, ElementParser>> elHandlers = new HashMap<QName, Map<UnitKey, ElementParser>>();
	Map<QName, Map<QName, Map<UnitKey, AttributeParser>>> attHandlers = new HashMap<QName, Map<QName, Map<UnitKey, AttributeParser>>>();

	public void register(QName qname, ElementParser<Unit<? extends Instruction>> handler) {
		for (Type t : handler.getClass().getGenericInterfaces()) {
			if (t instanceof ParameterizedType) {
				ParameterizedType pt = (ParameterizedType) t;
				if (pt.getRawType() instanceof Class && ElementParser.class.equals(pt.getRawType())) {
					ParameterizedType ct = (ParameterizedType) pt.getActualTypeArguments()[0];
					register(qname, new UnitKey((Class) ct.getActualTypeArguments()[0], (Class) ct.getRawType()), handler);
					return;
				}
			}
		}
	}

	@Override
	public void register(QName qname, UnitKey modelkey, ElementParser<Unit<? extends Instruction>> handler) {
		Map<UnitKey, ElementParser> entry = elHandlers.get(qname);
		if (entry == null) {
			entry = new HashMap<UnitKey, ElementParser>();
			elHandlers.put(qname, entry);
		}
		entry.put((UnitKey) modelkey, handler);

	}

	@Override
	public void unregister(QName qname, UnitKey modelkey) {

	}

	@Override
	public ElementParser<Unit<? extends Instruction>> retrieve(QName qname, Unit model) {
		UnitKey modelKey = new UnitKey((Class<? extends Instruction>) model.type(), (Class<? extends Unit<?>>) model.getClass());
		Map<UnitKey, ElementParser> entry = elHandlers.get(qname);
		if (entry != null) {
			if (modelKey != null) {
				ElementParser handler = entry.get(modelKey);
				if (handler == null) {
					for (Map.Entry<UnitKey, ElementParser> canidates : entry.entrySet()) {
						if (canidates.getKey().isAssignable(modelKey)) {
							handler = canidates.getValue();
							break;
						}
					}
				}
				return handler;
			} else {
				return entry.values().iterator().next();
			}
		}
		return null;
	}

	public void register(QName ename, QName aname, AttributeParser<Unit<? extends Instruction>> handler) {
		for (Type t : handler.getClass().getGenericInterfaces()) {
			if (t instanceof ParameterizedType) {
				ParameterizedType pt = (ParameterizedType) t;
				if (pt.getRawType() instanceof Class && ElementParser.class.equals(pt.getRawType())) {
					ParameterizedType ct = (ParameterizedType) pt.getActualTypeArguments()[0];
					register(ename, aname, new UnitKey((Class) ct.getActualTypeArguments()[0], (Class) ct.getRawType()), handler);
					return;
				}
			}
		}
	}

	@Override
	public void register(QName ename, QName aname, UnitKey modelkey, AttributeParser<Unit<? extends Instruction>> handler) {
		Map<QName, Map<UnitKey, AttributeParser>> eentry = attHandlers.get(ename);
		if (eentry == null) {
			eentry = new HashMap<QName, Map<UnitKey, AttributeParser>>();
			attHandlers.put(ename, eentry);
		}
		Map<UnitKey, AttributeParser> aentry = eentry.get(aname);
		if (aentry == null) {
			aentry = new HashMap<UnitKey, AttributeParser>();
			eentry.put(aname, aentry);
		}
		aentry.put((UnitKey) modelkey, handler);
	}

	@Override
	public void unregister(QName ename, QName aname, UnitKey modelkey) {

	}

	@Override
	public AttributeParser<Unit<? extends Instruction>> retrieve(QName ename, QName aname, Unit model) {
		UnitKey modelKey = new UnitKey((Class<? extends Instruction>) model.type(), (Class<? extends Unit<?>>) model.getClass());
		Map<QName, Map<UnitKey, AttributeParser>> eentry = attHandlers.get(ename);
		if (eentry != null) {
			Map<UnitKey, AttributeParser> aentry = eentry.get(aname);
			if (aentry != null) {
				if (modelKey != null) {
					AttributeParser handler = aentry.get(modelKey);
					if (handler == null) {
						for (Map.Entry<UnitKey, AttributeParser> canidates : aentry.entrySet()) {
							if (canidates.getKey().isAssignable(modelKey)) {
								handler = canidates.getValue();
								break;
							}
						}
					}
					return handler;
				} else {
					return aentry.values().iterator().next();
				}
			}
		}
		return null;
	}

	public static class UnitKey {
		final Class<? extends Instruction> instructionClass;
		final Class<? extends Unit<?>> unitClass;

		public UnitKey(Class<? extends Instruction> instructionClass, Class<? extends Unit<?>> unitClass) {
			this.unitClass = unitClass;
			this.instructionClass = instructionClass;
		}

		public boolean isAssignable(UnitKey key) {
			if (instructionClass.isAssignableFrom(key.instructionClass) && unitClass.isAssignableFrom(key.unitClass)) {
				return true;
			}
			return false;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((instructionClass == null) ? 0 : instructionClass.hashCode());
			result = prime * result + ((unitClass == null) ? 0 : unitClass.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			UnitKey other = (UnitKey) obj;
			if (instructionClass == null) {
				if (other.instructionClass != null)
					return false;
			} else if (!instructionClass.equals(other.instructionClass))
				return false;
			if (unitClass == null) {
				if (other.unitClass != null)
					return false;
			} else if (!unitClass.equals(other.unitClass))
				return false;
			return true;
		}

	}

}
