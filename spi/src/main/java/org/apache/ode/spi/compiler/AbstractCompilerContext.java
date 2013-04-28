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

import java.util.concurrent.locks.ReadWriteLock;

import javax.xml.bind.Binder;

import org.apache.ode.spi.compiler.Source.SourceType;
import org.apache.ode.spi.exec.executable.xml.Executable;
import org.w3c.dom.Node;

/**
 * Defines contextual compilation operations For a specific content type compiler instance.
 * 
 */
public abstract class AbstractCompilerContext<U> implements CompilerContext {

	private Source src;
	private CompilerPhase phase;
	private Compilation state;

	public void init(Source src, Compilation state) {
		this.src = src;
		this.phase = CompilerPhase.INITIALIZE;
		this.state = state;

	}

	@Override
	public CompilerPhase phase() {
		return phase;
	}

	public void setPhase(CompilerPhase phase) {
		this.phase = phase;
	}

	@Override
	public Source source() {
		return src;
	}

	String getContentType() {
		if (src.sourceType() == SourceType.INLINE) {
			return ((InlineSource) src).inlineContentType();
		}
		return src.getContentType();
	}

	/**
	 * Generic context shared amongst compiler instances participating in group compilation
	 * 
	 * @param id
	 * @return
	 */

	@Override
	public <S> S subContext(String id, Class<S> type) {
		return state.getSubContext(id, type);
	}

	@Override
	public Executable executable() {
		return state.getExecutable();
	}

	@Override
	public Binder<Node> executableBinder() {
		return state.getBinder();
	}

	@Override
	public ReadWriteLock executableLock() {
		return state.getExecutableLock();
	}

	@Override
	public void addWarning(Location location, String msg, Throwable t) {
		state.log(src, "warning", location, msg, t);
	}

	@Override
	public void addError(Location location, String msg, Throwable t) {
		state.log(src, "error", location, msg, t);
	}

	@Override
	public void terminate() {
		state.setTerminated(true);
	}

	@Override
	public void declareSource(String contentType, Location start, Location end) {
		if (CompilerPhase.DISCOVERY != phase) {
			addError(null, "Sources may only be added during the discovery phase", null);
			terminate();
			return;
		}
		state.declareSource(src, contentType, start, end);
	}

	public static interface Compilation {

		public <S> S getSubContext(String id, Class<S> type);

		public Executable getExecutable();

		public Binder<Node> getBinder();

		public ReadWriteLock getExecutableLock();

		public void log(Source src, String type, Location location, String msg, Throwable t);

		public void declareSource(Source src, String contentType, Location start, Location end);

		public void setTerminated(boolean value);

	}

}
