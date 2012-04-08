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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.xml.bind.Binder;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;

import org.apache.ode.runtime.build.SourceImpl.InlineSourceImpl;
import org.apache.ode.spi.compiler.AbstractCompiler;
import org.apache.ode.spi.compiler.AbstractCompilerContext.Compilation;
import org.apache.ode.spi.compiler.Location;
import org.apache.ode.spi.compiler.Source;
import org.apache.ode.spi.exec.Component.InstructionSet;
import org.apache.ode.spi.exec.xml.Executable;
import org.w3c.dom.Node;

public class CompilationImpl implements Compilation{
	private AtomicInteger srcIdCounter=new AtomicInteger();
	private Executable executable;
	private Binder<Node> binder;
	private final Set<InstructionSet> instructionSets = new HashSet<InstructionSet>();
	private final ReentrantReadWriteLock executableLock = new ReentrantReadWriteLock();
	private final Map<String, Object> subContext = new HashMap<String, Object>();
	boolean terminated = false;
	private final StringBuilder messages = new StringBuilder();
	private final ReentrantLock logLock = new ReentrantLock();
	private final Queue<InlineSourceImpl> addedSources = new ConcurrentLinkedQueue<InlineSourceImpl>();
	private final Map<String, AbstractCompiler<?,?>> compilers = new HashMap<String, AbstractCompiler<?,?>>();
	private JAXBContext jaxbContext;
	JAXBElement<Executable> execBase;

	public String nextSrcId() {
		return "s"+srcIdCounter.getAndIncrement();
	}
	
	public Map<String, Object> getSubContexts(){
		return subContext;
	}
	
	@Override
	public <S> S getSubContext(String id, Class<S> type){
		return (S)subContext.get(id);
	}
	@Override
	 public ReadWriteLock getExecutableLock(){
		return executableLock;
	}
	
	@Override
	public Executable getExecutable() {
		return executable;
	}

	public void setExecutable(Executable executable) {
		this.executable = executable;
	}

	@Override
	public Binder<Node> getBinder() {
		return binder;
	}

	public void setBinder(Binder<Node> binder) {
		this.binder = binder;
	}

	@Override
	public void log(Source src, String type, Location location, String msg, Throwable t) {
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
		logLock.lock();
		try {
			this.messages.append(messages);
		} finally {
			logLock.unlock();
		}

	}
	
	public StringBuilder getMessages(){
		return messages;
	}

	@Override
	public void declareSource(Source src, String contentType, Location start, Location end) {
		addedSources.add(new InlineSourceImpl((SourceImpl)src, contentType, start, end));

	}
	public boolean isTerminated() {
		return terminated;
	}

	@Override
	public void setTerminated(boolean value) {
		terminated = value;
	}

	
	public Queue<InlineSourceImpl> getAddedSources() {
		return addedSources;
	}

	public Set<InstructionSet> getInstructionSets() {
		return instructionSets;
	}

	public Map<String, AbstractCompiler<?,?>> getCompilers() {
		return compilers;
	}

	public JAXBContext getJaxbContext() {
		return jaxbContext;
	}

	public void setJaxbContext(JAXBContext jaxbContext) {
		this.jaxbContext = jaxbContext;
	}

	public JAXBElement<Executable> getExecBase() {
		return execBase;
	}

	public void setExecBase(JAXBElement<Executable> execBase) {
		this.execBase = execBase;
	}
}