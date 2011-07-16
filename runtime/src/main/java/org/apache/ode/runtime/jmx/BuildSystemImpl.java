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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.CommandInfo;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.xml.namespace.QName;

import org.apache.ode.api.Repository.ArtifactId;
import org.apache.ode.runtime.build.BuildExecutor;
import org.apache.ode.runtime.build.BuildSystem;
import org.apache.ode.runtime.build.DumpSources;
import org.apache.ode.spi.repo.Artifact;
import org.apache.ode.spi.repo.ArtifactDataSource;
import org.apache.ode.spi.repo.DataHandler;
import org.apache.ode.spi.repo.Repository;

public class BuildSystemImpl implements org.apache.ode.api.BuildSystem {

	@Inject
	Repository repo;
	@Inject
	Provider<ArtifactDataSource> dsProvider;

	private static final Logger log = Logger.getLogger(BuildSystem.class.getName());

	@Override
	public void build(ArtifactId artifactId) throws IOException {
		log.log(Level.FINE, "Build {0}", artifactId);
		QName qname = QName.valueOf(artifactId.getName());
		try {
			Artifact artifact = repo.read(qname, artifactId.getType() != null ? artifactId.getType() : BuildSystem.BUILDPLAN_MIMETYPE,
					artifactId.getVersion() != null ? artifactId.getVersion() : "1.0", Artifact.class);
			ArtifactDataSource ds = dsProvider.get();
			ds.configure(artifact);
			DataHandler dh = repo.getDataHandler(ds);
			CommandInfo info = dh.getCommand(BuildExecutor.BUILD_CMD);
			BuildExecutor exec = (BuildExecutor) dh.getBean(info);
			exec.build();
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	@Override
	public void build(byte[] contents) throws IOException {
		log.fine("Building from provided build plan");
		try {
			ArtifactDataSource ds = dsProvider.get();
			ds.configure(BuildSystem.BUILDPLAN_MIMETYPE, contents);
			DataHandler dh = repo.getDataHandler(ds);
			CommandInfo info = dh.getCommand(BuildExecutor.BUILD_CMD);
			BuildExecutor exec = (BuildExecutor) dh.getBean(info);
			exec.build();
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	public byte[][] dumpSources(ArtifactId artifactId, String targetName, String srcName) throws IOException {
		log.log(Level.FINE, "Dump {0} {1} {2}", new Object[] { artifactId, targetName, srcName });
		QName qname = QName.valueOf(artifactId.getName());
		try {
			Artifact artifact = repo.read(qname, artifactId.getType() != null ? artifactId.getType() : BuildSystem.BUILDPLAN_MIMETYPE,
					artifactId.getVersion() != null ? artifactId.getVersion() : "1.0", Artifact.class);
			ArtifactDataSource ds = dsProvider.get();
			ds.configure(artifact);
			DataHandler dh = repo.getDataHandler(ds);
			CommandInfo info = dh.getCommand(DumpSources.DUMPSRC_CMD);
			DumpSources dump = (DumpSources) dh.getBean(info);
			return dump.dump(targetName, srcName);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	public byte[][] dumpSources(byte[] contents, String targetName, String srcName) throws IOException {
		log.log(Level.INFO, "Dumping source from provided build plan {0} {1}", new Object[] { targetName, srcName });
		try {
			ArtifactDataSource ds = dsProvider.get();
			ds.configure(BuildSystem.BUILDPLAN_MIMETYPE, contents);
			DataHandler dh = repo.getDataHandler(ds);
			CommandInfo info = dh.getCommand(DumpSources.DUMPSRC_CMD);
			DumpSources dump = (DumpSources) dh.getBean(info);
			return dump.dump(targetName, srcName);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

}
