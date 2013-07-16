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
package org.apache.ode.data.memory.repo;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.ode.data.core.repo.RepoFileTypeMap;
import org.apache.ode.data.memory.repo.xml.ClassPath;
import org.apache.ode.data.memory.repo.xml.Directory;
import org.apache.ode.data.memory.repo.xml.Exclude;
import org.apache.ode.data.memory.repo.xml.Include;
import org.apache.ode.data.memory.repo.xml.Repository;
import org.apache.ode.data.memory.repo.xml.Resource;
import org.apache.ode.spi.repo.Artifact;
import org.apache.ode.spi.repo.DataContentHandler;

//since the RepoCache is basically a singleton this registry will be one too. This
//greatly simplifies DI of the CacheLoader/Writer
@Singleton
public class FileRepository extends Repository {
	@Inject
	RepoFileTypeMap fileTypeMap;

	Path defaultDirectory = null;

	private static final Logger log = Logger.getLogger(FileRepository.class.getName());

	ConcurrentHashMap<UUID, LocalArtifact> artifacts = new ConcurrentHashMap<>();
	boolean synced = false;
	boolean updateable = false;

	public Path getDefaultDirectory() {
		return defaultDirectory;
	}

	public void setArtifact(UUID key, LocalArtifact la) {
		artifacts.put(key, la);
	}

	public LocalArtifact getArtifact(UUID key) {
		return artifacts.get(key);
	}

	public LocalArtifact removeArtifact(UUID key) {
		return artifacts.remove(key);
	}

	public boolean isSynced() {
		return synced;
	}

	public boolean isUpdateable() {
		return updateable;
	}

	public void setUpdateable(boolean updateable) {
		this.updateable = updateable;
	}

	public class FileRepoUnMarshaller extends Unmarshaller.Listener {

		@Override
		public void beforeUnmarshal(Object target, Object parent) {
			//void beforeUnmarshal(Unmarshaller unmarshaller, Object parent) {
			//log.info(String.format("before Unmarshal called %s %s\n", target, parent));
		}

		public void addResource(Resource r, String path) throws IOException {
			URL file = Thread.currentThread().getContextClassLoader().getResource(path);
			if (file == null) {
				log.severe(String.format("resource not found %s", path));
			} else {
				byte[] contents = DataContentHandler.readStream(file.openStream());

				if (r.isAuto()) {
					//TODO invoke introspect command to glean uuid,urn,etc info
				}
				ClasspathArtifact la = new ClasspathArtifact();
				la.setPath(path);
				la.setId(UUID.randomUUID());
				try {
					la.setURI(r.getUri() != null ? r.getUri() : new URI(path));
				} catch (URISyntaxException e) {
					throw new IOException(e);
				}
				la.setContentType(r.getContentType() != null ? r.getContentType() : fileTypeMap.getContentType(r.getName()));
				la.setVersion(r.getVersion() != null ? r.getVersion() : "1.0");
				la.setContent(contents);
				log.fine(String.format("classpath resource %s included", path));
			}
		}

		//This method is called after all the properties (except IDREF) are unmarshalled for this object, 
		//but before this object is set to the parent object.
		@Override
		public void afterUnmarshal(Object target, Object parent) {
			//public void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
			//log.info(String.format("afterUnmarshal called %s %s\n", target, parent));

			//initially tried to setup classpath wildcard scanning but that was a fiasco. Explicitly reference classpath resources for now.
			try {
				if (target instanceof Repository) {
					for (ClassPath c : getClasspath()) {
						String baseLocation = c.getBase().endsWith("/") ? c.getBase().substring(0, c.getBase().length() - 1) : c.getBase();
						for (Resource r : c.getResource()) {
							log.fine(String.format("looking for resource  %s in base classpath directory %s", r.getName(), baseLocation));
							//List<URL> includesMatches = CPScanner.scanResources(new ResourceFilter().directoryName(baseLocation).resourceName(include.getInclude()));
							//todo sub in corn
							String path = baseLocation + "/" + r.getName();
							addResource(r, path);
						}
					}

					for (Resource r : getResource()) {
						log.fine(String.format("looking for resource  %s", r.getName()));
						addResource(r, r.getName());
					}

					for (final Directory d : getDirectory()) {
						Path basePath = null;
						if (d.getBase().isAbsolute()) {
							basePath = Paths.get(d.getBase());
						} else {
							basePath = Paths.get(d.getBase().getPath());
						}

						if (!Files.exists(basePath) || !Files.isDirectory((basePath))) {
							log.severe(String.format("invalid directory %s", basePath.toAbsolutePath()));
							continue;
						}

						if (d.isDefault()) {
							defaultDirectory = basePath;
						}

						for (Include i : d.getInclude()) {
							PathMatcher inc = FileSystems.getDefault().getPathMatcher(String.format("glob:%s", i.getInclude()));
							boolean include = false;
							for (Path file : Files.newDirectoryStream(basePath)) {			
								if (inc != null && inc.matches(file)) {
									include = true;

									for (Exclude e : d.getExclude()) {
										PathMatcher exc = FileSystems.getDefault().getPathMatcher(String.format("glob:%s", i.getInclude()));

										if (exc.matches(file)) {
											log.fine(String.format("filename %s matches exclude pattern %s excluding", file, e.getExclude()));
											include = false;
											break;
										}
									}
								}
								if (include) {
									FileArtifact la = new FileArtifact();
									la.setPath(file);
									byte[] contents = DataContentHandler.readStream(Files.newInputStream(file));
									if (i.isAuto()) {

									}
									la.setId(UUID.randomUUID());
									la.setURI(i.getUri() != null ? i.getUri() : file.toUri());
									la.setContentType(i.getContentType() != null ? i.getContentType() : fileTypeMap.getContentType(file.getFileName().toString()));
									la.setVersion(i.getVersion() != null ? i.getVersion() : "1.0");
									la.setContent(contents);
									log.fine(String.format("filename %s matches include pattern %s including", file, i.getInclude()));

								}
							}
						}
					}
				}

			} catch (IOException ie) {
				log.log(Level.SEVERE, "", ie);
			}
		}
	}

	public class FileRepoMarshaller extends Marshaller.Listener {
		// Invoked by Marshaller after it has created an formula of this object.

		@Override
		public void beforeMarshal(Object source) {
			//boolean beforeMarshal(Marshaller marshaller) {
			//log.info("beforeMarshal called\n");
			//return true;
		}

		@Override
		public void afterMarshal(Object source) {
			// Invoked by Marshaller after it has marshalled all properties of this object.
			//void afterMarshal(Marshaller marshaller) {
			//log.info("afterMarshal called\n");
		}
	}

	public static class LocalArtifact extends Artifact {

	}

	public static class ClasspathArtifact extends LocalArtifact {

		String path;

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

	}

	public static class FileArtifact extends LocalArtifact {

		boolean readOnly = false;
		Path path;

		public boolean isReadOnly() {
			return readOnly;
		}

		public void setReadOnly(boolean readOnly) {
			this.readOnly = readOnly;
		}

		public Path getPath() {
			return path;
		}

		public void setPath(Path path) {
			this.path = path;
		}

	}

}
