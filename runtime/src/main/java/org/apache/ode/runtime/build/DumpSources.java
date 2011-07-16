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
import org.apache.ode.runtime.build.xml.PreProcessor;
import org.apache.ode.runtime.build.xml.Target;
import org.apache.ode.runtime.exec.platform.PlatformImpl;
import org.apache.ode.spi.compiler.CompilerPass;
import org.apache.ode.spi.compiler.CompilerPhase;
import org.apache.ode.spi.compiler.InlineSource;
import org.apache.ode.spi.compiler.ParserException;
import org.apache.ode.spi.compiler.ParserUtils;
import org.apache.ode.spi.compiler.Source.SourceType;
import org.apache.ode.spi.exec.Component;
import org.apache.ode.spi.exec.Component.InstructionSet;
import org.apache.ode.spi.exec.xml.Executable;
import org.apache.ode.spi.exec.xml.Sources;
import org.apache.ode.spi.repo.Artifact;
import org.apache.ode.spi.repo.Repository;
import org.apache.ode.spi.repo.RepositoryException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class DumpSources implements CommandObject {

	public static final String DUMPSRC_CMD = "dumpSrc";

	@Inject
	Repository repo;

	DataHandler handler;

	@Override
	public void setCommandContext(String command, DataHandler handler) throws IOException {
		this.handler = handler;
	}

	public byte[][] dump(String targetName, String srcName) throws BuildException {
		JAXBElement<BuildPlan> root = null;
		try {
			root = (JAXBElement<BuildPlan>) handler.getContent();
		} catch (IOException ie) {
			throw new BuildException(ie);
		}
		BuildPlan plan = root.getValue();
		if (plan.getTarget().size() > 1 && targetName == null) {
			throw new BuildException("targetName must be specified if plan contains more than one target");
		}
		List<byte[]> srcs = new ArrayList<byte[]>();
		if (targetName != null) {
			for (Target target : plan.getTarget()) {
				if (targetName.equalsIgnoreCase(target.getName())) {
					byte[][] tsrc = loadSrc(srcName, target);
					for (byte[] src : tsrc) {
						srcs.add(src);
					}
					break;
				}
			}
		} else {
			for (Target target : plan.getTarget()) {
				byte[][] tsrc = loadSrc(srcName, target);
				for (byte[] src : tsrc) {
					srcs.add(src);
				}
			}
		}
		if (srcs.size() == 0) {
			throw new BuildException(String.format("Unable to find source %s for target %s", srcName != null ? srcName : "ALL", targetName != null ? targetName
					: "ALL"));
		}
		return srcs.toArray(new byte[srcs.size()][]);
	}

	byte[][] loadSrc(String srcName, Target target) throws BuildException {
		List<byte[]> srcs = new ArrayList<byte[]>();
		BuildSource main = target.getMain().getArtifact();
		if (srcName != null) {
			if ("s0".equals(srcName)) {
				srcs.add(preProcess(main));
			} else if (target.getIncludes() != null) {
				int i = 1;
				for (BuildSource inc : target.getIncludes().getArtifact()) {
					if (srcName.equals("s" + i++)) {
						srcs.add(preProcess(inc));
						break;
					}
				}
			}
		} else {
			srcs.add(preProcess(main));
			if (target.getIncludes() != null) {
				for (BuildSource inc : target.getIncludes().getArtifact()) {
					srcs.add(preProcess(inc));
				}
			}
		}

		return srcs.toArray(new byte[srcs.size()][]);
	}

	byte[] preProcess(BuildSource source) throws BuildException {
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
				BuildExecutor.preProcess(intermediate, source.getPreprocessor(), repo);
			}
			contents = ParserUtils.domToContent(intermediate);
		} catch (ParserException pe) {
			throw new BuildException(pe);
		}
		return contents;

	}

}
