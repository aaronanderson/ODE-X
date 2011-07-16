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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.activation.CommandObject;
import javax.activation.DataHandler;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.xml.bind.Binder;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.ode.runtime.build.SourceImpl.InlineSourceImpl;
import org.apache.ode.runtime.build.xml.BuildPlan;
import org.apache.ode.runtime.build.xml.BuildSource;
import org.apache.ode.runtime.build.xml.Java;
import org.apache.ode.runtime.build.xml.PreProcessor;
import org.apache.ode.runtime.build.xml.Target;
import org.apache.ode.runtime.build.xml.Xpath;
import org.apache.ode.runtime.build.xml.Xpath.Annotation;
import org.apache.ode.runtime.build.xml.Xslt;
import org.apache.ode.runtime.exec.platform.PlatformImpl;
import org.apache.ode.spi.compiler.CompilerPass;
import org.apache.ode.spi.compiler.CompilerPhase;
import org.apache.ode.spi.compiler.InlineSource;
import org.apache.ode.spi.compiler.ParserException;
import org.apache.ode.spi.compiler.ParserUtils;
import org.apache.ode.spi.compiler.Source.SourceType;
import org.apache.ode.spi.exec.Component;
import org.apache.ode.spi.exec.Component.InstructionSet;
import org.apache.ode.spi.exec.xml.Block;
import org.apache.ode.spi.exec.xml.Executable;
import org.apache.ode.spi.exec.xml.Instruction;
import org.apache.ode.spi.exec.xml.Sources;
import org.apache.ode.spi.repo.Artifact;
import org.apache.ode.spi.repo.Repository;
import org.apache.ode.spi.repo.RepositoryException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class BuildExecutor implements CommandObject {

	public static final String BUILD_CMD = "build";

	@Inject
	Repository repo;
	@Inject
	CompilersImpl compilers;
	@Inject
	PlatformImpl platform;

	DataHandler handler;

	@Override
	public void setCommandContext(String command, DataHandler handler) throws IOException {
		this.handler = handler;
	}

	public void build() throws BuildException {
		JAXBElement<BuildPlan> root = null;
		try {
			root = (JAXBElement<BuildPlan>) handler.getContent();
		} catch (IOException ie) {
			throw new BuildException(ie);
		}
		BuildPlan plan = root.getValue();
		for (Target target : plan.getTarget()) {
			processTarget(target);
		}
	}

	void processTarget(Target target) throws BuildException {
		List<CompilerContextImpl> contexts = new ArrayList<CompilerContextImpl>();
		Compilation compilation = new Compilation();

		BuildSource main = target.getMain().getArtifact();
		addCompiler(main.getContentType(), compilation);
		contexts.add(new CompilerContextImpl(preProcess(main, compilation.nextSrcId(), SourceType.MAIN), compilation));

		if (target.getIncludes() != null) {
			for (BuildSource src : target.getIncludes().getArtifact()) {
				addCompiler(src.getContentType(), compilation);
				contexts.add(new CompilerContextImpl(preProcess(src, compilation.nextSrcId(), SourceType.INCLUDE), compilation));
			}
		}
		// TODO make this multithreaded by using a threadpool and execute each pass in a runnable synchronizing on a countdown barrier
		for (CompilerPhase phase : CompilerPhase.values()) {
			switch (phase) {
			case DISCOVERY:
				executePass(contexts, phase, compilation);
				while (!compilation.getAddedSources().isEmpty()) {
					InlineSourceImpl src = compilation.getAddedSources().remove();
					addCompiler(src.inlineContentType(), compilation);
					CompilerContextImpl ctx = new CompilerContextImpl(src, compilation);
					contexts.add(ctx);
					executePass(ctx, CompilerPhase.INITIALIZE, compilation);
					executePass(ctx, CompilerPhase.DISCOVERY, compilation);
				}

				break;
			case EMIT:
				emitBase(contexts, compilation);
				executePass(contexts, phase, compilation);
				populateIds(compilation);
				break;
			default:
				executePass(contexts, phase, compilation);
				break;
			}

		}
		/*
		 * try { binder.marshal(execBase, execDoc); } catch (Exception jbe)
		 * { throw new BuildException(jbe); }
		 */
		byte[] contents = null;
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			Marshaller u = compilation.getJaxbContext().createMarshaller();
			u.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			u.marshal(compilation.getExecBase(), bos);
			contents = bos.toByteArray();
		} catch (Exception jbe) {
			throw new BuildException(jbe);
		}
		try {
			repo.create(target.getArtifact().getQname(), target.getArtifact().getContentType(), target.getArtifact().getVersion(), contents);
		} catch (Exception re) {
			throw new BuildException(re);
		}

	}

	void executePass(List<CompilerContextImpl> contexts, CompilerPhase phase, Compilation compilation) throws BuildException {
		for (CompilerContextImpl ctx : contexts) {
			executePass(ctx, phase, compilation);
		}
	}

	void executePass(CompilerContextImpl context, CompilerPhase phase, Compilation compilation) throws BuildException {
		context.setPhase(phase);
		CompilerImpl compiler = compilation.getCompilers().get(context.getContentType());
		for (CompilerPass pass : compiler.getCompilerPasses(phase)) {
			pass.compile(context);
		}
		if (compilation.isTerminated()) {
			throw new BuildException(compilation.getMessages().toString());
		}
	}

	void emitBase(List<CompilerContextImpl> contexts, Compilation compilation) throws BuildException {
		StringBuilder contextPath = new StringBuilder();
		contextPath.append("org.apache.ode.spi.exec.xml");
		for (String path : compilation.getJaxbContexts()) {
			contextPath.append(':');
			contextPath.append(path);
		}

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder db;
		try {
			db = factory.newDocumentBuilder();
		} catch (ParserConfigurationException pe) {
			throw new BuildException(pe);
		}
		Executable exec = new Executable();
		Sources srcs = new Sources();
		exec.setSources(srcs);
		for (CompilerContextImpl impl : contexts) {
			if (!(impl.source() instanceof InlineSource)) {
				srcs.getSources().add(impl.source().id());
			}
		}

		compilation.setExecutable(exec);
		org.apache.ode.spi.exec.xml.ObjectFactory of = new org.apache.ode.spi.exec.xml.ObjectFactory();
		compilation.setExecBase(of.createExecutable(exec));

		Binder<Node> binder;
		Document execDoc;
		try {
			compilation.setJaxbContext(JAXBContext.newInstance(contextPath.toString()));
			binder = compilation.getJaxbContext().createBinder();
			compilation.setBinder(binder);
			execDoc = db.newDocument();
			binder.marshal(compilation.getExecBase(), execDoc);
		} catch (Exception jbe) {
			throw new BuildException(jbe);
		}

	}

	void populateIds(Compilation compilation) {
		Executable exec = compilation.getExecutable();
		int blockId = 0;
		for (Block b : exec.getBlock()) {
			String bid = "b" + blockId++;
			b.setBlc(bid);
			int insId = 0;
			for (Object o : b.getBody()) {
				if (o instanceof Instruction) {
					((Instruction) o).setIns(bid + "i" + insId++);
				}
			}

		}
	}

	void addCompiler(String contentType, Compilation compilation) throws BuildException {
		if (!compilation.getCompilers().containsKey(contentType)) {
			CompilerImpl impl = (CompilerImpl) compilers.getCompiler(contentType);
			if (impl == null) {
				throw new BuildException(String.format("Unable to locate compiler form contentType %s", contentType));
			}
			compilation.getCompilers().put(contentType, impl);
			for (QName iset : impl.getInstructionSets()) {
				Component c = platform.getComponent(iset);
				if (c == null) {
					throw new BuildException(String.format("Unsupported instructionset %s", iset));
				}
				for (InstructionSet is : c.instructionSets()) {
					compilation.getJaxbContexts().add(is.getJAXBContextPath());
				}

			}

			for (Map.Entry<String, Provider<?>> p : impl.getSubContexts().entrySet()) {
				if (!compilation.getSubContext().containsKey(p.getKey())) {
					compilation.getSubContext().put(p.getKey(), p.getValue().get());
				}

			}
		}
	}

	SourceImpl preProcess(BuildSource source, String id, SourceType sourceType) throws BuildException {
		Artifact artifact = null;
		try {
			artifact = repo.read(source.getQname(), source.getContentType(), source.getVersion(), Artifact.class);
		} catch (RepositoryException re) {
			throw new BuildException(re);
		}
		byte[] contents = artifact.getContent();
		try {
			Document intermediate = ParserUtils.inlineLocation(contents);
			if (source.getPreprocessor() != null) {
				preProcess(intermediate, source.getPreprocessor(), repo);
			}
			contents = ParserUtils.domToContent(intermediate);
		} catch (ParserException pe) {
			throw new BuildException(pe);
		}
		return new SourceImpl(artifact.getQName(), artifact.getContentType(), artifact.getVersion(), artifact.getCheckSum(), contents, id, sourceType);

	}

	public static void preProcess(Document src, PreProcessor processor, Repository repo) throws BuildException {
		if (processor instanceof Xpath) {
			Xpath xpath = (Xpath) processor;
			for (Annotation a : xpath.getAnnotation()) {
				for (Object o : a.getAny()) {

				}
			}
		} else if (processor instanceof Java) {

		} else if (processor instanceof Xslt) {

		}
	}

}
