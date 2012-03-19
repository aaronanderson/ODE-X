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
package org.apache.ode.spi.compiler;

import javax.inject.Singleton;

import org.apache.ode.spi.xml.AttributeHandler;
import org.apache.ode.spi.xml.ElementHandler;
import org.apache.ode.spi.xml.HandlerException;
import org.apache.ode.spi.xml.HandlerRegistry;

@Singleton
public abstract class AbstractXMLCompiler<M, X extends HandlerException, C extends XMLCompilerContext<M, X>, E extends ElementHandler<? extends M, C, X>, A extends AttributeHandler<? extends M, C, X>, H extends HandlerRegistry<M, C, X, E, A>>
		extends AbstractCompiler<M, C> implements XMLCompiler<M, X, C, E, A, H> {

	private H registry;

	public AbstractXMLCompiler(H registry) {
		this.registry = registry;
	}

	@Override
	public H handlerRegistry(Class<H> type) {
		return registry;
	}

	@Override
	abstract public C newContext();

}