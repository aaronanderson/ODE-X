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
import java.util.Map;

import javax.xml.bind.Binder;

import org.apache.ode.spi.compiler.CompilerContext;
import org.apache.ode.spi.exec.xml.Executable;
import org.w3c.dom.Node;

public class CompilerContextImpl implements CompilerContext {

	private Executable executable;
	private Binder<Node> binder;
	private Map<String, Object> subContext;
	boolean terminated;
	private StringBuilder messages;

	public CompilerContextImpl(Executable executable, Binder<Node> binder, Map<String, Object> subContext) {
		this.executable = executable;
		this.binder = binder;
		this.subContext = subContext;
		terminated = false;
		messages = new StringBuilder();

	}

	@Override
	public <C> C getSubContext(String id) {
		return (C) subContext.get(id);
	}

	@Override
	public Executable executable() {
		return executable;
	}

	@Override
	public Binder<Node> executableBinder() {
		return binder;
	}

	@Override
	public void addWarning(String msg, Throwable t) {
		messages.append("warning: ");
		messages.append(msg);
		StringWriter sw = new StringWriter();
		t.printStackTrace(new PrintWriter(sw));
		messages.append(sw.toString());

	}

	@Override
	public void addError(String msg, Throwable t) {
		messages.append("error: ");
		messages.append(msg);
		StringWriter sw = new StringWriter();
		t.printStackTrace(new PrintWriter(sw));
		messages.append(sw.toString());
	}

	public StringBuilder getMessages() {
		return messages;
	}

	@Override
	public void terminate() {
		terminated = true;
	}

}
