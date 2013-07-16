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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.cache.Cache.Entry;
import javax.cache.configuration.Factory;
import javax.cache.integration.CacheWriter;
import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.ode.data.memory.repo.FileRepoCacheWriterFactory.FileRepoCacheWriter;
import org.apache.ode.data.memory.repo.FileRepository.FileArtifact;
import org.apache.ode.data.memory.repo.FileRepository.LocalArtifact;
import org.apache.ode.spi.repo.Artifact;
import org.apache.ode.spi.repo.RepositoryException;

/*
 This is a challenging DI scenario. 
 
 The FileRepository XML should be read only after DI. This is because
 the files content type could be discovered by file extension which is set on the repo during component registration after DI is completed. 
 Having the CacheLoader being a DI instance will probably be a common scenario, like cacheloading from an injected JPA entity manager.
  
 However, the cache is built before DI configuration is complete so it itself can be injected. This creates some what of a paradox, trying to inject a DI instance into a DI instance before DI is available. 
 The CacheLoaderFactory cannot be a DI instance because it is created during DI configuration. However, a cacheload provider instance can be
 passed in and the CacheLoaderFactory and that can be used to create the DI CacheLoader instance. 
 */
public class FileRepoCacheWriterFactory implements Factory<FileRepoCacheWriter> {

	private static final Logger log = Logger.getLogger(FileRepoCacheWriterFactory.class.getName());
	/*	di - automatic
		Repository -> cache
		cache reader-> file Repository, but file repository is from xml and may be in different formats, needs to be configured post setup
		potential cache writer-> file Repository

		catalyst -manual online
		file repository is parsed from xml, reference established, and provided to the caches
		*/
	//AtomicReference<FileRepository> fileRepo = new AtomicReference<FileRepository>();
	Provider<FileRepoCacheWriter> writeProvider;

	public FileRepoCacheWriterFactory(Provider<FileRepoCacheWriter> writeProvider) {
		this.writeProvider = writeProvider;
	}

	/*
	public void setFileRepository(FileRepository fileRepo) {
		this.fileRepo.set(fileRepo);
	}*/

	@Override
	public FileRepoCacheWriter create() {
		FileRepoCacheWriter writer = writeProvider.get();
		//pass by reference
		//writer.setFileRepository(fileRepo);
		return writer;

	}

	public static class FileRepoCacheWriter implements CacheWriter<UUID, Artifact> {

		private static final Logger log = Logger.getLogger(FileRepoCacheWriter.class.getName());
		/*	di - automatic
			Repository -> cache
			cache reader-> file Repository, but file repository is from xml and may be in different formats, needs to be configured post setup
			potential cache writer-> file Repository

			catalyst -manual online
			file repository is parsed from xml, reference established, and provided to the caches
			*/
		//
		@Inject
		FileRepository fileRepo;

		//AtomicReference<FileRepository> fileRepo;

		/*public void setFileRepository(AtomicReference<FileRepository> fileRepo) {
			this.fileRepo = fileRepo;
		}*/

		@Override
		public void write(Entry<? extends UUID, ? extends Artifact> entry) {
			if (entry instanceof FileArtifact) {
				if (fileRepo == null /*|| fileRepo.get() == null*/) {
					log.warning(String.format("fileRepository not set, ignoring write %s", entry));
					return;
				}
				fileRepo/*.get()*/.setArtifact(entry.getKey(), (LocalArtifact) entry.getValue());
				FileArtifact la = (FileArtifact) entry;
				if (!la.isReadOnly() && la.getContent() != null) {
					if (la.getPath() == null) {
						if (fileRepo.getDefaultDirectory() == null) {
							log.warning(String.format("fileRepository default directory not set and file path is not set, ignoring write %s", entry));
							return;
						}
						//a.setPath(fileRepo.getDefaultDirectory().resolve(la.getId().toString())) ;
					}
					try {
						//see if contents have changed
						if (Files.exists(la.getPath())) {
							FileChannel channel = FileChannel.open(la.getPath());
							ByteBuffer bb = ByteBuffer.allocate((int) channel.size());
							channel.read(bb);
							byte[] existingContents = bb.array();
							channel.close();
							String csum = Artifact.checkSumSHA1(existingContents);
							if (la.getCheckSum().equals(csum)) {
								log.fine(String.format("checksums match, no update needed. %s", la.getPath()));
							}

						}
						FileChannel channel = FileChannel.open(la.getPath());
						ByteBuffer bb = ByteBuffer.wrap(la.getContent());
						channel.write(bb);
						channel.close();
						log.fine(String.format("Artifact written %s %s", la, la.getPath()));
					} catch (IOException | RepositoryException e) {
						log.log(Level.SEVERE, String.format("Error saving artifact %s %s", la, la.getPath(), e));
					}
				}
			}

		}

		@Override
		public void writeAll(Collection<Entry<? extends UUID, ? extends Artifact>> entries) {
			for (Entry<? extends UUID, ? extends Artifact> entry : entries) {
				write(entry);
			}

		}

		@Override
		public void delete(Object key) {
			if (key instanceof UUID) {
				if (fileRepo == null /*|| fileRepo.get() == null*/) {
					log.warning(String.format("fileRepository not set, ignoring delete %s", key));
					return;
				}
				LocalArtifact la = fileRepo/*.get()*/.removeArtifact((UUID) key);
				if (la instanceof FileArtifact) {
					FileArtifact fa = (FileArtifact) la;
					if (fa.getPath() != null) {
						if (((FileArtifact) la).isReadOnly()) {
							try {
								Files.delete(fa.getPath());
								log.fine(String.format("Artifact deleted %s %s", la, fa.getPath()));
							} catch (IOException ie) {
								log.log(Level.SEVERE, String.format("Error deleting artifact %s %s", la, fa.getPath()), ie);
							}
						} else {
							log.fine(String.format("artifact %s is read only", fa));
						}
					} else {
						log.severe(String.format("artifact %s does not have path set, cannot delete", fa));
					}
				}
			}

		}

		@Override
		public void deleteAll(Collection<?> keys) {
			for (Object key : keys) {
				delete(key);
			}

		}

	}

}
