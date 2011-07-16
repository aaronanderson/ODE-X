package org.apache.ode.runtime.build;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.xml.bind.Binder;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;

import org.apache.ode.runtime.build.SourceImpl.InlineSourceImpl;
import org.apache.ode.spi.compiler.InlineSource;
import org.apache.ode.spi.exec.xml.Executable;
import org.w3c.dom.Node;

public class Compilation {
	private AtomicInteger srcIdCounter=new AtomicInteger();
	private Executable executable;
	private Binder<Node> binder;
	private final Set<String> jaxbContexts = new HashSet<String>();
	private final ReentrantReadWriteLock executableLock = new ReentrantReadWriteLock();
	private final Map<String, Object> subContext = new HashMap<String, Object>();
	boolean terminated = false;
	private final StringBuilder messages = new StringBuilder();
	private final ReentrantLock logLock = new ReentrantLock();
	private final Queue<InlineSourceImpl> addedSources = new ConcurrentLinkedQueue<InlineSourceImpl>();
	private final Map<String, CompilerImpl> compilers = new HashMap<String, CompilerImpl>();
	private JAXBContext jaxbContext;
	JAXBElement<Executable> execBase;

	public String nextSrcId() {
		return "s"+srcIdCounter.getAndIncrement();
	}
	
	public Executable getExecutable() {
		return executable;
	}

	public void setExecutable(Executable executable) {
		this.executable = executable;
	}

	public Binder<Node> getBinder() {
		return binder;
	}

	public void setBinder(Binder<Node> binder) {
		this.binder = binder;
	}

	public ReentrantReadWriteLock getExecutableLock() {
		return executableLock;
	}

	public Map<String, Object> getSubContext() {
		return subContext;
	}

	public boolean isTerminated() {
		return terminated;
	}

	public void setTerminated(boolean value) {
		terminated = value;
	}

	public StringBuilder getMessages() {
		return messages;
	}

	public ReentrantLock getLogLock() {
		return logLock;
	}

	public Queue<InlineSourceImpl> getAddedSources() {
		return addedSources;
	}

	public Set<String> getJaxbContexts() {
		return jaxbContexts;
	}

	public Map<String, CompilerImpl> getCompilers() {
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