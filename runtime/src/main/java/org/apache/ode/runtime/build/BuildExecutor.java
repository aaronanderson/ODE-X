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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.CommandObject;
import javax.activation.DataHandler;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.xml.bind.Binder;
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
import org.apache.ode.runtime.exec.JAXBRuntimeUtil;
import org.apache.ode.runtime.exec.platform.PlatformImpl;
import org.apache.ode.spi.compiler.AbstractCompiler;
import org.apache.ode.spi.compiler.AbstractCompilerContext;
import org.apache.ode.spi.compiler.AbstractHandlerRegistry;
import org.apache.ode.spi.compiler.AbstractXMLCompiler;
import org.apache.ode.spi.compiler.AbstractXMLCompilerContext;
import org.apache.ode.spi.compiler.CompilerPass;
import org.apache.ode.spi.compiler.CompilerPhase;
import org.apache.ode.spi.compiler.InlineSource;
import org.apache.ode.spi.compiler.ParserException;
import org.apache.ode.spi.compiler.ParserUtils;
import org.apache.ode.spi.compiler.Source.SourceType;
import org.apache.ode.spi.exec.Component;
import org.apache.ode.spi.exec.Component.InstructionSet;
import org.apache.ode.spi.exec.xml.Block;
import org.apache.ode.spi.exec.xml.BlockAddress;
import org.apache.ode.spi.exec.xml.Executable;
import org.apache.ode.spi.exec.xml.Instruction;
import org.apache.ode.spi.exec.xml.InstructionAddress;
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
		List<AbstractCompilerContext<?>> contexts = new ArrayList<AbstractCompilerContext<?>>();
		CompilationImpl compilation = new CompilationImpl();

		BuildSource main = target.getMain().getArtifact();
		AbstractCompiler<?, ?> compiler = addCompiler(main.getContentType(), compilation);
		SourceImpl src = preProcess(main, compilation.nextSrcId(), compiler.getPragmaNS(), SourceType.MAIN);
		contexts.add(newCompilerContext(src, compiler, compilation));

		if (target.getIncludes() != null) {
			for (BuildSource bsrc : target.getIncludes().getArtifact()) {
				compiler = addCompiler(bsrc.getContentType(), compilation);
				src = preProcess(bsrc, compilation.nextSrcId(), compiler.getPragmaNS(), SourceType.INCLUDE);
				contexts.add(newCompilerContext(src, compiler, compilation));
			}
		}
		// TODO make this multithreaded by using a threadpool and execute each pass in a runnable synchronizing on a countdown barrier
		for (CompilerPhase phase : CompilerPhase.values()) {
			switch (phase) {
			case DISCOVERY:
				executePass(contexts, phase, compilation);
				while (!compilation.getAddedSources().isEmpty()) {
					InlineSourceImpl isrc = compilation.getAddedSources().remove();
					compiler = addCompiler(isrc.inlineContentType(), compilation);
					AbstractCompilerContext<?> ctx = newCompilerContext(src, compiler, compilation);
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

	void executePass(List<AbstractCompilerContext<?>> contexts, CompilerPhase phase, CompilationImpl compilation) throws BuildException {
		for (AbstractCompilerContext<?> ctx : contexts) {
			executePass(ctx, phase, compilation);
		}
	}

	void executePass(AbstractCompilerContext<?> context, CompilerPhase phase, CompilationImpl compilation) throws BuildException {
		context.setPhase(phase);
		AbstractCompiler<?, ?> compiler = compilation.getCompilers().get(context.source().getContentType());
		for (CompilerPass pass : compiler.getCompilerPasses(phase)) {
			pass.compile(context);
		}
		if (compilation.isTerminated()) {
			throw new BuildException(compilation.getMessages().toString());
		}
	}

	void emitBase(List<AbstractCompilerContext<?>> contexts, CompilationImpl compilation) throws BuildException {
		
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
		for (AbstractCompilerContext<?> impl : contexts) {
			if (!(impl.source() instanceof InlineSource)) {
				srcs.getSources().add(impl.source().srcRef());
			}
		}

		compilation.setExecutable(exec);
		org.apache.ode.spi.exec.xml.ObjectFactory of = new org.apache.ode.spi.exec.xml.ObjectFactory();
		compilation.setExecBase(of.createExecutable(exec));

		Binder<Node> binder;
		Document execDoc;
		try {
			compilation.setJaxbContext(JAXBRuntimeUtil.executableJAXBContextByPath(compilation.getInstructionSets()));
			binder = compilation.getJaxbContext().createBinder();
			compilation.setBinder(binder);
			execDoc = db.newDocument();
			binder.marshal(compilation.getExecBase(), execDoc);
		} catch (Exception jbe) {
			throw new BuildException(jbe);
		}

	}

	void populateIds(CompilationImpl compilation) {
		Executable exec = compilation.getExecutable();
		int blockId = 0;
		for (Block b : exec.getBlock()) {
			String bid = "b" + blockId++;
			b.setBlc(new BlockAddress(bid));
			int insId = 0;
			for (Object o : b.getInstructions()) {
				if (o instanceof Instruction) {
					String iid=bid + "i" + insId++;
					((Instruction) o).setIns(new InstructionAddress(iid));
				}
			}

		}
	}

	AbstractCompilerContext<?> newCompilerContext(SourceImpl src, AbstractCompiler<?, ?> compiler, CompilationImpl state) {
		AbstractCompilerContext<?> ctx = (AbstractCompilerContext<?>) compiler.newContext();
		if (compiler instanceof AbstractXMLCompiler) {
			AbstractXMLCompiler<?,?,?,?,?,?> xcompiler = (AbstractXMLCompiler<?,?,?,?,?,?>) compiler;
			AbstractXMLCompilerContext<?,?,?,?,?> xctx = (AbstractXMLCompilerContext<?,?,?,?,?>)ctx;
			xctx.init(src, state, (AbstractHandlerRegistry)xcompiler.handlerRegistry(null));
		} else {
			ctx.init(src, state);
		}

		return ctx;
	}

	AbstractCompiler<?, ?> addCompiler(String contentType, CompilationImpl compilation) throws BuildException {
		AbstractCompiler<?, ?> impl = compilation.getCompilers().get(contentType);
		if (impl == null) {
			impl = compilers.getCompiler(contentType, AbstractCompiler.class);
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
					compilation.getInstructionSets().add(is);
				}

			}

			for (Map.Entry<String, Provider<?>> p : impl.getSubContexts().entrySet()) {
				if (!compilation.getSubContexts().containsKey(p.getKey())) {
					compilation.getSubContexts().put(p.getKey(), p.getValue().get());
				}

			}
		}
		return impl;
	}

	SourceImpl preProcess(BuildSource source, String id, Set<String> pragmaNS, SourceType sourceType) throws BuildException {
		Artifact artifact = null;
		try {
			artifact = repo.read(source.getQname(), source.getContentType(), source.getVersion(), Artifact.class);
		} catch (RepositoryException re) {
			throw new BuildException(re);
		}
		byte[] contents = artifact.getContent();
		try {
			Document intermediate = ParserUtils.inlineLocation(contents, pragmaNS);
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
