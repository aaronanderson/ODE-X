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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.ode.data.core.repo.RepoFileTypeMap;
import org.apache.ode.data.memory.repo.xml.Directory;
import org.apache.ode.data.memory.repo.xml.Repository;
import org.apache.ode.spi.repo.Artifact;

//since the RepoCache is basically a singleton this registry will be one too. This
//greatly simplifies DI of the CacheLoader/Writer
@Singleton
public class FileRepository extends Repository {
	@Inject
	RepoFileTypeMap fileTypeMap;

	private static final Logger log = Logger.getLogger(FileRepository.class.getName());

	ConcurrentHashMap<UUID, LocalArtifact> artifacts = new ConcurrentHashMap<>();
	boolean synced = false;
	boolean updateable = false;

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

		//This method is called after all the properties (except IDREF) are unmarshalled for this object, 
		//but before this object is set to the parent object.
		@Override
		public void afterUnmarshal(Object target, Object parent) {
			//public void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
			//log.info(String.format("afterUnmarshal called %s %s\n", target, parent));
			if (target instanceof Repository) {
				for (Directory d : directory) {
					//for (inc)
					String base = d.getBase();
					if (base.startsWith("classpath://")) {
						try {
							base = base.substring(12);
							//base = !base.endsWith("/") ? base.concat("/") : base;
							//Too cool!!! http://stackoverflow.com/questions/13848333/accessing-files-in-specific-folder-in-classpath-using-java
							log.fine(String.format("attempting to process files in classpath directory %s", base));
							//Enumeration<URL> files =Thread.currentThread().getContextClassLoader().getResources( base );
							//List<URL> resources = CPScanner.scanResources(new PackageNameFilter(base), new ResourceNameFilter("*.xml"));
							//if (url == null) {
							//	log.severe(String.format("directory location %s not found in classpath", base));
								//continue;
						//	}
							ClassLoader loader = getClass().getClassLoader();
					        InputStream in = loader.getResourceAsStream(".");
					        BufferedReader rdr = new BufferedReader(new InputStreamReader(in));
					        String line;
					        while ((line = rdr.readLine()) != null) {
					            System.out.println("file: " + line);
					        }
					        rdr.close();
							//while (files.hasMoreElements()){
							//	System.out.println(files.nextElement());
							//}
							/*BufferedReader br = new BufferedReader(new InputStreamReader(is));
							String fileName;
							while ((fileName = br.readLine()) != null) {
								log.severe(String.format("processing %s ", fileName));
								for (Excludes exclude : d.getExcludes()) {
									if (exclude.getExclude().matcher(fileName).matches()) {
										log.fine(String.format("filename %s matches exclude pattern %s excluding", fileName, exclude.getExclude().pattern()));
										continue;
									}
								}
								for (Includes include : d.getIncludes()) {
									if (include.getInclude().matcher(fileName).matches()) {
										LocalArtifact la = new LocalArtifact();
										la.setBaseDirectory(d.getBase());
										la.setPath(fileName);
										la.setId(UUID.randomUUID());
										la.setURI(include.getUri() != null ? include.getUri() : new URI(d.getBase() + "/" + fileName));
										la.setContentType(include.getContentType() != null ? include.getContentType() : fileTypeMap.getContentType(fileName));
										la.setVersion(include.getVersion() != null ? include.getVersion() : "1.0");
										la.setContent(DataContentHandler.readStream(Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName)));
										log.fine(String.format("filename %s matches include pattern %s including", fileName, include.getInclude().pattern()));
									}
								}
								log.fine(String.format("filename %s didn't match any includes, skipping", fileName));
							}*/
						} catch (Exception e) {
							log.log(Level.SEVERE, "", e);
						}

					} else if (base.startsWith("file://")) {
						//TODO
						log.severe(String.format("Unsupported base format %s", base));
					} else {
						log.severe(String.format("Unsupported base format %s", base));
					}
				}
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

		boolean transientArtifact = true;
		String baseDirectory;
		String path;

		public boolean isTransient() {
			return transientArtifact;
		}

		public void setTransient(boolean transientArtifact) {
			this.transientArtifact = transientArtifact;
		}

		public String getBaseDirectory() {
			return baseDirectory;
		}

		public void setBaseDirectory(String baseDirectory) {
			this.baseDirectory = baseDirectory;
		}

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

	}

}
