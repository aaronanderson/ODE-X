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
package org.apache.ode.di.guice.memory;

import java.util.logging.Logger;

import org.apache.ode.di.guice.core.DIModule;
import org.apache.ode.di.guice.core.DIModule.OperationAnnotationProcessor;
import org.apache.ode.di.guice.core.JSR250Module;
import org.apache.ode.runtime.memory.exec.platform.ContextOperations;

import com.google.inject.AbstractModule;

public class MemoryModule extends AbstractModule {

	public static Logger log = Logger.getLogger(MemoryModule.class.getName());

	protected void configure() {
		install(new JSR250Module());
		install(new DIModule(new OperationAnnotationProcessor()));
		bind(ContextOperations.class);
	}

}