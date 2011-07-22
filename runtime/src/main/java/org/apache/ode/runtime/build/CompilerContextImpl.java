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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.locks.ReadWriteLock;

import javax.xml.bind.Binder;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.ode.runtime.build.SourceImpl.InlineSourceImpl;
import org.apache.ode.spi.compiler.CompilerContext;
import org.apache.ode.spi.compiler.CompilerPhase;
import org.apache.ode.spi.compiler.InlineSource;
import org.apache.ode.spi.compiler.Location;
import org.apache.ode.spi.compiler.Parser;
import org.apache.ode.spi.compiler.ParserException;
import org.apache.ode.spi.compiler.ParserRegistry;
import org.apache.ode.spi.compiler.Source;
import org.apache.ode.spi.compiler.Unit;
import org.apache.ode.spi.compiler.Source.SourceType;
import org.apache.ode.spi.exec.xml.Executable;
import org.apache.ode.spi.exec.xml.Instruction;
import org.w3c.dom.Node;

public class CompilerContextImpl implements CompilerContext {

	private SourceImpl src;
	private CompilerPhase phase;
	private ParserRegistry registry;
	private Compilation state;

	public CompilerContextImpl(SourceImpl src, Compilation state) {
		this.src = src;
		this.phase = CompilerPhase.INITIALIZE;
		this.registry = state.getCompilers().get(src.getContentType()).getParserRegistry();
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

	@Override
	public <C> C subContext(String id) {
		return (C) state.getSubContext().get(id);
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
		log("warning", location, msg, t);
	}

	void log(String type, Location location, String msg, Throwable t) {
		StringBuilder messages = new StringBuilder();
		messages.append(type);
		messages.append(": Source ");
		messages.append(src.getQName());
		messages.append(":");
		messages.append(src.getContentType());
		messages.append(" ");
		if (location != null) {
			messages.append("[");
			messages.append(location.getLine());
			messages.append(",");
			messages.append(location.getColumn());
			messages.append("] ");
		}
		messages.append(msg);
		if (t != null) {
			messages.append(" ");
			StringWriter sw = new StringWriter();
			t.printStackTrace(new PrintWriter(sw));
			messages.append(sw.toString());
		}
		messages.append("\n");
		state.getLogLock().lock();
		try {
			state.getMessages().append(messages);
		} finally {
			state.getLogLock().unlock();
		}

	}

	@Override
	public void addError(Location location, String msg, Throwable t) {
		log("error", location, msg, t);
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
		state.getAddedSources().add(new InlineSourceImpl(src, contentType, start, end));

	}

	@Override
	public <U extends Unit<? extends Instruction>> void parseContent(XMLStreamReader input, U subModel) throws XMLStreamException, ParserException {
		if (input.getEventType() != XMLStreamConstants.START_ELEMENT) {
			return;
		}
		Parser<U> handler = (Parser<U>) registry.retrieve(input.getName(), subModel);
		if (handler != null) {
			handler.parse(input, subModel, this);
		} else {
			throw new ParserException(String.format("[%d,%d] Unable to locate handler for %s", input.getLocation().getLineNumber(), input.getLocation()
					.getColumnNumber(), input.getName()));

		}
	}

}
