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
package org.apache.ode.runtime.jmx;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.ode.api.Repository.ArtifactId;
import org.apache.ode.spi.compiler.ParserUtils;
import org.apache.ode.spi.exec.Platform;
import org.apache.ode.spi.exec.Target;
import org.apache.ode.spi.repo.Artifact;
import org.apache.ode.spi.repo.Repository;
import org.w3c.dom.Document;

public class PlatformImpl implements org.apache.ode.api.Platform {

	@Inject
	Repository repo;
	@Inject
	Platform platform;

	private static final Logger log = Logger.getLogger(PlatformImpl.class.getName());

	@Override
	public byte[] setup(ArtifactId executable) throws IOException {
		String type = executable.getType() != null ? executable.getType() : Platform.EXEC_MIMETYPE;
		String version = executable.getVersion() != null ? executable.getVersion() : "1.0";
		log.log(Level.FINE, "Loading setup data of executable name {0}: type: {1} version: {2]", new Object[] { executable.getName(), type, version });
		try {
			QName qname = QName.valueOf(executable.getName());
			Artifact artifact = repo.read(qname, type, version, Artifact.class);
			Document doc = platform.setup(artifact);
			if (doc == null) {
				return null;
			}
			return ParserUtils.domToContent(doc);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	@Override
	public void install(String id, ArtifactId executable, byte[] installData, String[] targets) throws IOException {
		String pid = id != null ? id : executable.getName();
		String type = executable.getType() != null ? executable.getType() : Platform.EXEC_MIMETYPE;
		String version = executable.getVersion() != null ? executable.getVersion() : "1.0";
		log.log(Level.FINE, "Installing program {0} using executable name {1}: type: {2} version: {3} {4} custom installation data", new Object[] { pid,
				executable.getName(), type, version, installData != null ? "with" : "without" });
		try {
			QName idQname = QName.valueOf(pid);
			QName execQname = QName.valueOf(executable.getName());
			Artifact artifact = repo.read(execQname, type, version, Artifact.class);
			Document installDoc = null;
			if (installData != null) {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				factory.setNamespaceAware(true);
				DocumentBuilder db = factory.newDocumentBuilder();
				ByteArrayInputStream bis = new ByteArrayInputStream(installData);
				installDoc = db.parse(bis);
			}
			Target[] target = null;
			if (targets != null) {

			}

			platform.install(idQname, installDoc, target);
		} catch (Exception e) {
			throw new IOException(e);
		}

	}

	@Override
	public Program programInfo(String id) throws IOException {
		return null;
	}

	@Override
	public Process start(String pid, String[] targets) throws IOException {
		log.log(Level.FINE, "Starting program {0}", pid);
		QName idQname = QName.valueOf(pid);
		Target[] target = null;
		if (targets != null) {

		}
		try {
			platform.start(idQname, target);
		} catch (Exception e) {
			throw new IOException(e);
		}
		return null;
	}

	@Override
	public void stop(String id, String[] targets) throws IOException {
		log.log(Level.FINE, "Stopping program {0}", id);
		Target[] target = null;
		if (targets != null) {

		}

	}

	@Override
	public void uninstall(String id, String[] targets) throws IOException {
		Target[] target = null;
		if (targets != null) {

		}

	}

}
