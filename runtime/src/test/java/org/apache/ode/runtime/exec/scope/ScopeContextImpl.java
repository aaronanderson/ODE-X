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
package org.apache.ode.runtime.exec.scope;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.ode.runtime.exec.platform.ScopeContext;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.mycila.inject.jsr250.Jsr250;
import com.mycila.inject.jsr250.Jsr250Injector;

/*
 * http://code.google.com/p/google-guice/wiki/CustomScopes
 */
public abstract class ScopeContextImpl implements ScopeContext {

	ThreadLocalState scope = null;

	@Inject
	Jsr250Injector injector;

	static class ThreadLocalState {
		Map<Class<?>, Map<String, Object>> namedInstances;
		Map<Class<?>, Object> singletonInstances;
		Set<Object> instances;
	}

	public abstract ThreadLocal<ThreadLocalState> getThreadLocal();

	@Override
	public void create() {
		scope = new ThreadLocalState();
		scope.namedInstances = new HashMap<Class<?>, Map<String, Object>>();
		scope.singletonInstances = new HashMap<Class<?>, Object>();
		scope.instances = new HashSet<>();
	}

	@Override
	public void begin() {
		if (getThreadLocal().get() != null) {
			throw new IllegalStateException("Already in scope");
		}
		getThreadLocal().set(scope);
	}

	@Override
	public void end() {
		if (getThreadLocal().get() == null) {
			throw new IllegalStateException("Not in scope");
		}
		getThreadLocal().remove();
	}

	@Override
	public <T> T newInstance(Class<T> clazz) {
		return injector.getInstance(clazz);
	}

	@Override
	public void wrap(Object unmanaged) {

	}

	@Override
	public void destroy() {

		for (Object entry2 : scope.instances) {
			Jsr250.preDestroy(entry2);
		}
		scope = null;
	}

	public abstract static class GuiceScopeImpl implements Scope {

		@Override
		public <T> Provider<T> scope(final Key<T> key, final Provider<T> provider) {
			return new Provider<T>() {
				public T get() {
					T current = null;
					ThreadLocalState tscope = getThreadLocal().get();
					Class<?> type = key.getTypeLiteral().getRawType();
					Named named = type.getAnnotation(Named.class);
					if (named != null) {
						String name = named.value();
						Map<String, Object> entry = tscope.namedInstances.get(type);
						if (entry != null) {
							current = (T) entry.get(name);
						} else {
							entry = new HashMap<String, Object>();
							tscope.namedInstances.put(type, entry);
						}
						if (current == null) {
							current = provider.get();
							entry.put(name, current);
							tscope.instances.add(current);
						}
						return current;

					} else if (isSingleton(type) || getSingletons().contains(type)) {
						current = (T) tscope.singletonInstances.get(type);
						if (current == null) {
							current = provider.get();
							tscope.singletonInstances.put(type, current);
							tscope.instances.add(current);
						}
					} else {
						current = provider.get();
						tscope.instances.add(current);
					}

					return current;
				}
			};

		}

		public abstract ThreadLocal<ThreadLocalState> getThreadLocal();

		public abstract Set<Class<?>> getSingletons();
		
		public abstract boolean isSingleton(Class<?> clazz);
	}

}
