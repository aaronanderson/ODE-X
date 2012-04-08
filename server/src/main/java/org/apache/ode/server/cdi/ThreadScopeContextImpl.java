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
package org.apache.ode.server.cdi;

import java.lang.annotation.Annotation;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.ode.runtime.exec.platform.ScopeContext.ExecutableScopeContext;
import org.apache.ode.spi.exec.ThreadScope;

/*
 * I really hate thread locals but it seems that is the only way to get non-trivial custom scopes to work.
 */
@Dependent
public class ThreadScopeContextImpl extends ScopeContextImpl implements ExecutableScopeContext {

	static ThreadLocal<ThreadLocalState> threadLocal = new ThreadLocal<ThreadLocalState>();

	@Inject
	Provider<InstructionScopeContextImpl> insScope;

	@Override
	public ThreadLocal<ThreadLocalState> getThreadLocal() {
		return threadLocal;
	}

	public static class ThreadCDIContextImpl extends CDIContextImpl {

		@Override
		public ThreadLocal<ThreadLocalState> getThreadLocal() {
			return threadLocal;
		}

		@Override
		public Class<? extends Annotation> getScope() {
			return ThreadScope.class;
		}

	}

}
