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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import javax.cache.Cache.Entry;
import javax.cache.configuration.Factory;
import javax.cache.integration.CacheLoader;
import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.ode.data.memory.repo.FileRepoCacheLoaderFactory.FileRepoCacheLoader;
import org.apache.ode.data.memory.repo.FileRepository.LocalArtifact;
import org.apache.ode.spi.repo.Artifact;

/*
 This is a challenging DI scenario. 
 
 The FileRepository XML should be read only after DI. This is because
 the files content type could be discovered by file extension which is set on the repo during component registration after DI is completed. 
 Having the CacheLoader being a DI instance will probably be a common scenario, like cacheloading from an injected JPA entity manager.
  
 However, the cache is built before DI configuration is complete so it itself can be injected. This creates some what of a paradox, trying to inject a DI instance into a DI instance before DI is available. 
 The CacheLoaderFactory cannot be a DI instance because it is created during DI configuration. However, a cacheload provider instance can be
 passed in and the CacheLoaderFactory and that can be used to create the DI CacheLoader instance. 
 */
public class FileRepoCacheLoaderFactory implements Factory<FileRepoCacheLoader> {

	private static final Logger log = Logger.getLogger(FileRepoCacheLoaderFactory.class.getName());
	/*	di - automatic
		Repository -> cache
		cache reader-> file Repository, but file repository is from xml and may be in different formats, needs to be configured post setup
		potential cache writer-> file Repository

		catalyst -manual online
		file repository is parsed from xml, reference established, and provided to the caches
		*/
	//AtomicReference<FileRepository> fileRepo = new AtomicReference<FileRepository>();
	Provider<FileRepoCacheLoader> loadProvider;

	public FileRepoCacheLoaderFactory(Provider<FileRepoCacheLoader> loadProvider) {
		this.loadProvider = loadProvider;
	}

	/*public void setFileRepository(FileRepository fileRepo) {
		this.fileRepo.set(fileRepo);
	}*/

	@Override
	public FileRepoCacheLoader create() {
		FileRepoCacheLoader loader = loadProvider.get();
		//pass by reference
		//loader.setFileRepository(fileRepo);
		return loader;

	}

	public static class FileRepoCacheLoader implements CacheLoader<UUID, Artifact> {

		private static final Logger log = Logger.getLogger(FileRepoCacheLoader.class.getName());
		/*	di - automatic
			Repository -> cache
			cache reader-> file Repository, but file repository is from xml and may be in different formats, needs to be configured post setup
			potential cache writer-> file Repository

			catalyst -manual online
			file repository is parsed from xml, reference established, and provided to the caches
			*/
		/*AtomicReference<FileRepository> fileRepo;
		
		public void setFileRepository(AtomicReference<FileRepository> fileRepo) {
			this.fileRepo = fileRepo;
		}*/
		@Inject
		FileRepository fileRepo;

		public Entry<UUID, Artifact> load(UUID key) {
			if (fileRepo == null /*|| fileRepo.get() == null*/) {
				log.warning(String.format("fileRepository not set, ignoring load %s", key));
				return null;
			}
			LocalArtifact lartifact = fileRepo./*.get()*/getArtifact(key);
			//TODO load artifact
			if (lartifact != null) {
				return new RepoEntry(key, lartifact);
			}
			return null;
		}

		public Map<UUID, Artifact> loadAll(Iterable<? extends UUID> keys) {
			if (fileRepo == null /* || fileRepo.get() == null*/) {
				log.warning(String.format("fileRepository not set, ignoring loadAll %s", keys));
				return null;
			}
			Map<UUID, Artifact> entries = new HashMap<UUID, Artifact>();
			for (UUID key : keys) {
				LocalArtifact lartifact = fileRepo./*.get()*/getArtifact(key);
				//TODO load artifact
				if (lartifact != null) {
					entries.put(key, lartifact);
				}
			}
			return entries;
		}

		public class RepoEntry implements Entry<UUID, Artifact> {
			UUID storageKey;
			Artifact storageValue;

			public RepoEntry(UUID storageKey, Artifact storageValue) {
				this.storageKey = storageKey;
				this.storageValue = storageValue;
			}

			public UUID getKey() {
				return storageKey;
			}

			public Artifact getValue() {
				return storageValue;
			}

			@Override
			public <T> T unwrap(Class<T> clazz) {
				return null;
			}

		}
	}

}
