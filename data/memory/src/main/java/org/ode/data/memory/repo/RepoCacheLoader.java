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
package org.ode.data.memory.repo;

import java.util.HashMap;
import java.util.Map;

import javax.cache.Cache.Entry;
import javax.cache.CacheLoader;
import javax.inject.Inject;

import org.apache.ode.spi.repo.Artifact;
import org.ode.data.memory.repo.FileRepository.LocalArtifact;

public class RepoCacheLoader implements CacheLoader<EntryKey, Artifact> {

	@Inject
	FileRepository fileRepo;
	
	public Entry<EntryKey, Artifact> load(EntryKey key) {
		LocalArtifact lartifact = fileRepo.getArtifact(key);
		//TODO load artifact
		if (lartifact != null) {
			return new RepoEntry(key, lartifact);
		}
		return null;
	}

	public Map<EntryKey, Artifact> loadAll(Iterable<? extends EntryKey> keys) {
		Map<EntryKey, Artifact> entries = new HashMap<EntryKey, Artifact>();
		for (EntryKey key : keys) {
			LocalArtifact lartifact = fileRepo.getArtifact(key);
			//TODO load artifact
			if (lartifact != null) {
				entries.put(key, lartifact);
			}
		}
		return entries;
	}

	public class RepoEntry implements Entry<EntryKey, Artifact> {
		EntryKey storageKey;
		Artifact storageValue;

		public RepoEntry(EntryKey storageKey, Artifact storageValue) {
			this.storageKey = storageKey;
			this.storageValue = storageValue;
		}

		public EntryKey getKey() {
			return storageKey;
		}

		public Artifact getValue() {
			return storageValue;
		}

	}

}
