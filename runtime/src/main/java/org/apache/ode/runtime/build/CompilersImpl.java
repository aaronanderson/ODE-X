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
package org.apache.ode.runtime.build;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Singleton;

import org.apache.ode.spi.compiler.CompilerContext;
import org.apache.ode.spi.compiler.Compiler;
import org.apache.ode.spi.compiler.Compilers;
import org.apache.ode.spi.compiler.XMLCompiler;
import org.apache.ode.spi.compiler.XMLCompilerContext;
import org.apache.ode.spi.xml.AttributeHandler;
import org.apache.ode.spi.xml.ElementHandler;
import org.apache.ode.spi.xml.HandlerException;
import org.apache.ode.spi.xml.HandlerRegistry;

@Singleton
public class CompilersImpl implements Compilers {

	Map<String, Compiler<?, ?>> compilers = new ConcurrentHashMap<String, Compiler<?, ?>>();
	Map<String, XMLCompiler<?, ?, ?, ?, ?, ?>> xmlCompilers = new ConcurrentHashMap<String, XMLCompiler<?, ?, ?, ?, ?, ?>>();

	@Override
	public <M, C extends CompilerContext, P extends Compiler<M, C>> void register(P compiler, String contentType) {
		if (compiler instanceof XMLCompiler) {
			xmlCompilers.put(contentType, (XMLCompiler<?, ?, ?, ?, ?, ?>) compiler);
		} else {
			compilers.put(contentType, compiler);
		}

	}

	@Override
	public <M, C extends CompilerContext, P extends Compiler<M, C>> void unregister(String contentType, Class<P> type) {
		// TODO Auto-generated method stub

	}

	@Override
	public <M, C extends CompilerContext, P extends Compiler<M, C>> P getCompiler(String contentType, Class<P> type) {
		P compiler = (P) compilers.get(contentType);
		if (compiler == null) {
			compiler = (P) xmlCompilers.get(contentType);
		}
		return compiler;
	}

	@Override
	public <M, X extends HandlerException, C extends XMLCompilerContext<M, X>, E extends ElementHandler<? extends M, C, X>, A extends AttributeHandler<? extends M, C, X>, H extends HandlerRegistry<M, C, X, E, A>, P extends XMLCompiler<M, X, C, E, A, H>> P getXMLCompiler(
			String contentType, Class<P> type) {
		return (P) xmlCompilers.get(contentType);
	}

}
