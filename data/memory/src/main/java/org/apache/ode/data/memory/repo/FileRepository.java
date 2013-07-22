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
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.cache.Cache;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.ode.data.core.repo.RepoFileTypeMap;
import org.apache.ode.data.memory.repo.RepositoryImpl.RepoCache;
import org.apache.ode.data.memory.repo.xml.ClassPath;
import org.apache.ode.data.memory.repo.xml.Directory;
import org.apache.ode.data.memory.repo.xml.Exclude;
import org.apache.ode.data.memory.repo.xml.Include;
import org.apache.ode.data.memory.repo.xml.Index;
import org.apache.ode.data.memory.repo.xml.IndexEntry;
import org.apache.ode.data.memory.repo.xml.IndexMode;
import org.apache.ode.data.memory.repo.xml.ObjectFactory;
import org.apache.ode.data.memory.repo.xml.Repository;
import org.apache.ode.data.memory.repo.xml.Resource;
import org.apache.ode.spi.repo.Artifact;
import org.apache.ode.spi.repo.DataContentHandler;

//since the RepoCache is basically a singleton this registry will be one too. This
//greatly simplifies DI of the CacheLoader/Writer
@Singleton
public class FileRepository {
	@Inject
	RepoFileTypeMap fileTypeMap;

	@Inject
	@RepoCache
	Cache<UUID, Artifact> repoCache;

	FileIndex fileIndex;

	private static final Logger log = Logger.getLogger(FileRepository.class.getName());
	public static JAXBContext FILEREPO_JAXBCONTEXT = null;

	static {
		try {
			FILEREPO_JAXBCONTEXT = JAXBContext.newInstance("org.apache.ode.data.memory.repo.xml");
		} catch (JAXBException je) {
			log.log(Level.SEVERE, "", je);
		}
	}

	boolean updateable = false;

	public FileIndex getIndex() {
		return fileIndex;
	}

	public static class FileIndex {
		IndexMode mode = IndexMode.NONE;
		Map<UUID, IndexEntry> entries = new HashMap<>();
		ReadWriteLock lock = new ReentrantReadWriteLock();
		Path indexFile = null;
		Path indexDirectory = null;

		public static IndexEntry toIndexEntry(Artifact artifact) {
			IndexEntry ie = new IndexEntry();
			ie.setId(artifact.getId());
			ie.setUri(artifact.getURI());
			ie.setCollection(artifact.getCollection());
			ie.setVersion(artifact.getVersion());
			ie.setContentType(artifact.getContentType());
			if (artifact instanceof FileArtifact) {
				Path p = ((FileArtifact) artifact).getPath();
				if (p != null) {
					ie.setPath(p.toAbsolutePath().toUri());
				}
			}
			return ie;
		}

		public static Artifact toLocalArtifact(IndexEntry entry) {
			if (entry == null) {
				return null;
			}
			LocalArtifact la = null;
			if (entry.getPath() != null) {
				la = new FileArtifact();
				((FileArtifact) la).setPath(Paths.get(entry.getPath()));
			} else {
				la = new LocalArtifact();
			}
			la.setId(entry.getId());
			la.setURI(entry.getUri());
			la.setCollection(entry.getCollection());
			la.setVersion(entry.getVersion());
			la.setContentType(entry.getContentType());
			return la;
		}

		public void clearIndex() throws IOException {
			Lock wlock = lock.writeLock();
			wlock.lock();
			try (DirectoryStream<Path> ds = Files.newDirectoryStream(indexDirectory)) {
				for (Path file : ds) {
					Files.delete(file);
				}
				entries.clear();
			} finally {
				wlock.unlock();
			}
		}

		public void cleanIndex() throws IOException {
			Lock wlock = lock.writeLock();
			wlock.lock();
			try (DirectoryStream<Path> ds = Files.newDirectoryStream(indexDirectory)) {
				for (Path file : ds) {
					String fileName = file.getFileName().toString();
					if (entries.get(fileName.substring(0, fileName.lastIndexOf('.'))) == null) {
						Files.delete(file);
					}
				}
				entries.clear();
			} finally {
				wlock.unlock();
			}
		}

		public void storeArtifactInIndex(UUID key, Artifact la) {

			Lock wlock = lock.writeLock();
			wlock.lock();
			try {
				Path contents = indexDirectory.resolve(String.format("%s.idx", key.toString()));
				Files.write(contents, la.getContent());
				entries.put(key, toIndexEntry(la));
				write();
			} catch (IOException ie) {
				log.log(Level.SEVERE, "", ie);
			} finally {
				wlock.unlock();
			}
		}

		public Artifact loadArtifactFromIndex(UUID key) {
			Lock rlock = lock.readLock();
			rlock.lock();
			try {
				Path contents = indexDirectory.resolve(String.format("%s.idx", key.toString()));
				if (!Files.exists(contents)) {
					return null;
				}
				byte[] fileContents = Files.readAllBytes(contents);
				Artifact a = toLocalArtifact(entries.get(key));
				a.setContent(fileContents);
				return a;
			} catch (IOException ie) {
				log.log(Level.SEVERE, "", ie);
				return null;
			} finally {
				rlock.unlock();
			}
		}

		public Artifact removeArtifactFromIndex(UUID key) {
			Lock wlock = lock.writeLock();
			wlock.lock();
			try {
				Path contents = indexDirectory.resolve(String.format("%s.idx", key.toString()));
				if (!Files.exists(contents)) {
					return null;
				}
				IndexEntry e = entries.remove(key);
				if (e == null) {
					return null;
				}
				Artifact a = toLocalArtifact(e);
				Files.delete(contents);
				write();
				return a;
			} catch (IOException ie) {
				log.log(Level.SEVERE, "", ie);
				return null;
			} finally {
				wlock.unlock();
			}
		}

		void read() throws IOException {
			if (!Files.exists(indexFile)) {
				return;
			}
			try {
				Unmarshaller u = FILEREPO_JAXBCONTEXT.createUnmarshaller();
				JAXBElement<Index> index = (JAXBElement<Index>) u.unmarshal(indexFile.toFile());
				Map<UUID, IndexEntry> newEntries = new HashMap<>();
				for (IndexEntry i : index.getValue().getEntry()) {
					newEntries.put(i.getId(), i);
				}
				entries = newEntries;
			} catch (JAXBException je) {
				throw new IOException(je);
			}
		}

		void write() throws IOException {
			try {
				Marshaller m = FILEREPO_JAXBCONTEXT.createMarshaller();
				ObjectFactory of = new ObjectFactory();
				JAXBElement<Index> index = of.createIndex(of.createIndex());
				for (IndexEntry i : entries.values()) {
					index.getValue().getEntry().add(i);
				}
				m.marshal(index, indexFile.toFile());

			} catch (JAXBException je) {
				throw new IOException(je);
			}
		}

	}

	// most of the time the FileRepository will be read in with other configurations and 
	//will not be read in as a standalone document.
	public void loadRepositoryCache(InputStream xmlFile) throws IOException {
		if (xmlFile != null) {
			try {
				JAXBContext jc = JAXBContext.newInstance("org.apache.ode.data.memory.repo.xml");
				Unmarshaller u = jc.createUnmarshaller();
				u.setListener(new FileRepoUnMarshaller());
				u.unmarshal(xmlFile);

			} catch (JAXBException je) {
				throw new IOException(je);
			}
		} else {
			throw new IOException("InputStream is null");
		}
	}

	public void loadRepositoryCache(String configLocation) throws IOException {
		ClassLoader currentCL = Thread.currentThread().getContextClassLoader();
		InputStream is = currentCL.getResourceAsStream(configLocation);
		if (is != null) {
			loadRepositoryCache(is);
		} else {
			throw new IOException(String.format("Unable to locate config file %s on classpath\n", configLocation));
		}
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
				Path fileName = Paths.get(path).getFileName();
				String contentType = r.getContentType() != null ? r.getContentType() : fileName != null ? fileTypeMap.getContentType(fileName.toString()) : null;
				if (contentType == null) {
					throw new IOException(String.format("Unknown content type for artifact %s", la));
				}
				la.setContentType(contentType);
				la.setVersion(r.getVersion() != null ? r.getVersion() : "1.0");
				la.setContent(contents);
				if (fileIndex.mode == IndexMode.NONE) { //if there is no index and no read through then store in cache
					repoCache.put(la.getId(), la);
				} else {
					fileIndex.storeArtifactInIndex(la.getId(), la);
				}
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
					Repository repo = (Repository) target;

					if (IndexMode.NONE != repo.getIndex()) {
						FileIndex newIndex = new FileIndex();
						newIndex.mode = repo.getIndex();
						if (repo.getIndexPath().isAbsolute()) {
							newIndex.indexDirectory = Paths.get(repo.getIndexPath());
						} else {
							newIndex.indexDirectory = Paths.get(repo.getIndexPath().getPath());
						}

						if (!Files.exists(newIndex.indexDirectory)) {
							log.info(String.format("index directory %s does not exist, creating", newIndex.indexDirectory));
							Files.createDirectories(newIndex.indexDirectory);
						}
						newIndex.indexFile = newIndex.indexDirectory.resolve("index.xml");
						if (fileIndex != null) {
							log.warning(String.format("indexDirectory previously set %s, overriding %s", fileIndex.indexDirectory, newIndex.indexDirectory));
						}
						fileIndex = newIndex;
						if (newIndex.mode == IndexMode.UPDATE) {
							fileIndex.read();
						}

					}

					for (ClassPath c : repo.getClasspath()) {
						String baseLocation = c.getBase().endsWith("/") ? c.getBase().substring(0, c.getBase().length() - 1) : c.getBase();
						for (Resource r : c.getResource()) {
							log.fine(String.format("looking for resource  %s in base classpath directory %s", r.getName(), baseLocation));
							//List<URL> includesMatches = CPScanner.scanResources(new ResourceFilter().directoryName(baseLocation).resourceName(include.getInclude()));
							//todo sub in corn
							String path = baseLocation + "/" + r.getName();
							addResource(r, path);
						}
					}

					for (Resource r : repo.getResource()) {
						log.fine(String.format("looking for resource  %s", r.getName()));
						addResource(r, r.getName());
					}

					for (final Directory d : repo.getDirectory()) {
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

						for (Include i : d.getInclude()) {
							PathMatcher inc = FileSystems.getDefault().getPathMatcher(String.format("glob:%s", i.getInclude()));
							boolean include = false;
							try (DirectoryStream<Path> ds = Files.newDirectoryStream(basePath)) {
								for (Path file : ds) {
									if (inc != null && inc.matches(basePath.relativize(file))) {
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
										String contentType = i.getContentType() != null ? i.getContentType() : fileTypeMap.getContentType(file.getFileName().toString());
										if (contentType == null) {
											throw new IOException(String.format("Unknown content type for artifact %s", la));
										}
										la.setContentType(contentType);
										la.setVersion(i.getVersion() != null ? i.getVersion() : "1.0");
										la.setContent(contents);
										if (fileIndex.mode == IndexMode.NONE) { //if there is no index and no read through then store in cache
											repoCache.put(la.getId(), la);
										} else {
											fileIndex.storeArtifactInIndex(la.getId(), la);
										}
										log.fine(String.format("filename %s matches include pattern %s including", file, i.getInclude()));

									}
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
