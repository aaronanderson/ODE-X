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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

import org.apache.ode.runtime.build.xml.BuildPlan;
import org.apache.ode.runtime.build.xml.BuildSource;
import org.apache.ode.runtime.build.xml.PreProcessor;
import org.apache.ode.runtime.build.xml.Target;
import org.apache.ode.runtime.exec.platform.PlatformImpl;
import org.apache.ode.spi.compiler.CompilerPass;
import org.apache.ode.spi.compiler.CompilerPhase;
import org.apache.ode.spi.compiler.Source;
import org.apache.ode.spi.compiler.Source.SourceType;
import org.apache.ode.spi.exec.Component;
import org.apache.ode.spi.exec.xml.Executable;
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
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder db;
		try {
			db = factory.newDocumentBuilder();
		} catch (ParserConfigurationException pe) {
			throw new BuildException(pe);
		}
		BuildPlan plan = root.getValue();
		for (Target target : plan.getTarget()) {
			List<Source> srcs = new ArrayList<Source>();
			Map<String, CompilerImpl> compilerCache = new HashMap<String, CompilerImpl>();
			Set<String> jaxbContexts = new HashSet<String>();
			Map<String, Object> subContexts = new HashMap<String, Object>();
			BuildSource main = target.getMain().getArtifact();
			srcs.add(preProcess(main, SourceType.MAIN));
			addCompiler(main, compilerCache, jaxbContexts, subContexts);
			if (target.getIncludes() != null) {
				for (BuildSource src : target.getIncludes().getArtifact()) {
					srcs.add(preProcess(src, SourceType.INCLUDE));
					addCompiler(src, compilerCache, jaxbContexts, subContexts);
				}
			}
			StringBuilder contextPath = new StringBuilder();
			contextPath.append("org.apache.ode.spi.exec.xml");
			for (String path : jaxbContexts) {
				contextPath.append(':');
				contextPath.append(path);
			}
			Executable exec = new Executable();
			org.apache.ode.spi.exec.xml.ObjectFactory of = new org.apache.ode.spi.exec.xml.ObjectFactory();
			JAXBElement<Executable> execBase = of.createExecutable(exec);
			JAXBContext jc;

			Binder<Node> binder;
			Document execDoc;
			try {
				jc = JAXBContext.newInstance(contextPath.toString());
				binder = jc.createBinder();
				execDoc = db.newDocument();
				binder.marshal(execBase, execDoc);
			} catch (Exception jbe) {
				throw new BuildException(jbe);
			}
			CompilerContextImpl ctx = new CompilerContextImpl(exec, binder, subContexts);

			for (CompilerPhase phase : CompilerPhase.values()) {
				for (Source src : srcs) {
					CompilerImpl compiler = compilerCache.get(src.getContentType());
					for (CompilerPass pass : compiler.getCompilerPasses(phase)) {
						pass.compile(phase, ctx, src);
					}
				}
			}
			/*
			 * try { binder.marshal(execBase, execDoc); } catch (Exception jbe)
			 * { throw new BuildException(jbe); }
			 */
			byte[] contents = null;
			try {
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				Marshaller u = jc.createMarshaller();
				u.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
				u.marshal(execBase, bos);
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
	}

	void addCompiler(BuildSource src, Map<String, CompilerImpl> compilerCache, Set<String> jaxbContexts, Map<String, Object> subContexts) throws BuildException {
		if (!compilerCache.containsKey(src.getContentType())) {
			CompilerImpl impl = (CompilerImpl) compilers.getCompiler(src.getContentType());
			if (impl == null) {
				throw new BuildException(String.format("Unable to locate compiler form contentType %s", src.getContentType()));
			}
			compilerCache.put(src.getContentType(), impl);
			for (QName iset : impl.getInstructionSets()) {
				Component c = platform.getComponent(iset);
				if (c == null) {
					throw new BuildException(String.format("Unsupported instructionset %s", iset));
				}
				jaxbContexts.add(c.jaxbContextPath());
			}

			for (Map.Entry<String, Provider<?>> p : impl.getSubContexts().entrySet()) {
				if (!subContexts.containsKey(p.getKey())) {
					subContexts.put(p.getKey(), p.getValue().get());
				}

			}
		}
	}

	Source preProcess(BuildSource source, SourceType sourceType) throws BuildException {
		Artifact artifact = null;
		try {
			artifact = repo.read(source.getQname(), source.getContentType(), source.getVersion(), Artifact.class);
		} catch (RepositoryException re) {
			throw new BuildException(re);
		}
		byte[] contents = artifact.getContent();
		if (source.getPreprocessor() != null) {
			PreProcessor processor = source.getPreprocessor();
		}
		return new SourceImpl(artifact.getQName(), artifact.getContentType(), artifact.getVersion(), artifact.getCheckSum(), contents, sourceType);

	}

}
