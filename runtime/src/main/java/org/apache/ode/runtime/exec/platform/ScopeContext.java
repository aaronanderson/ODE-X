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
package org.apache.ode.runtime.exec.platform;

public interface ScopeContext {

	public void create();

	<T> T newInstance(Class<T> clazz);

	/*
	 * This could be used to expose JAXB created objects to the DI container.
	 * Currently this is done using a custom JAXB Object factory registered with a proprietary unmarshaller property
	 * Instead a JAXB Unmarshaller listener could be registered and in the beforeMarshall method wrap the JAXB object.
	 * This way the object would be managed, i.e inject, post/destroy methods, etc.
	 */
	public void wrap(Object unmanaged);

	public void begin();

	public void end();

	public void destroy();

	public static interface ExecutableScopeContext extends ScopeContext {

	}

	public static interface ProgramScopeContext extends ScopeContext {

	}

	public static interface ProcessScopeContext extends ScopeContext {

	}

	public static interface ThreadScopeContext extends ScopeContext {

	}

	public static interface InstructionScopeContext extends ScopeContext {

	}

}
