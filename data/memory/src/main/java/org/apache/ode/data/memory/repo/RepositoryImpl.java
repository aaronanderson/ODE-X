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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.cache.Cache;
import javax.cache.Cache.Entry;
import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.inject.Singleton;

import org.apache.ode.data.core.repo.RepositoryBaseImpl;
import org.apache.ode.data.memory.repo.xml.IndexEntry;
import org.apache.ode.spi.repo.Artifact;
import org.apache.ode.spi.repo.Criteria;
import org.apache.ode.spi.repo.RepositoryException;

@Singleton
public class RepositoryImpl extends RepositoryBaseImpl {

	public static final String REPOSITORY_NAMESPACE = "http://ode.apache.org/data/memory/repository";

	@Inject
	@RepoCache
	Cache<UUID, Artifact> repoCache;

	@Inject
	FileRepository fileRepo;

	@Override
	protected void createArtifact(Artifact artifact) throws RepositoryException {
		if (artifact.getId() == null || artifact.getURI() == null || artifact.getContentType() == null) {
			throw new RepositoryException(String.format("Missing mandatory artifact attributes: id: %s uri: %s contentType: %s ", artifact.getId(), artifact.getURI(),
					artifact.getContentType()));
		}

		repoCache.put(artifact.getId(), artifact);
	}

	@Override
	protected void storeArtifact(Artifact artifact) throws RepositoryException {
		repoCache.put(artifact.getId(), artifact);
	}

	@Override
	public <R> void delete(R criteria) throws RepositoryException {
		List<Artifact> artifacts = loadArtifacts(criteria, 2);
		if (artifacts.size() == 0) {
			throw new RepositoryException(String.format("Artifact %s does not exists", criteria));
		}
		if (artifacts.size() > 1) {
			throw new RepositoryException(String.format("More than one Artifact exists for criteria %s", criteria));
		}

		if (!repoCache.remove(artifacts.get(0).getId())) {
			throw new RepositoryException(String.format("Artifact %s does not exists", criteria));
		}

	}

	@Qualifier
	@Target({ ElementType.FIELD, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface RepoCache {

		public String value() default "";

	}

	@Override
	protected <R> List<Artifact> loadArtifacts(R criteria, long limit) throws RepositoryException {
		List<Artifact> artifacts = new ArrayList<Artifact>();
		if (criteria instanceof UUID) {
			Artifact artifact = repoCache.get((UUID) criteria);//will read through to index if present
			if (artifact != null) {
				artifacts.add(artifact);
			}
		} else if (criteria instanceof URI) {
			URI uri = (URI) criteria;
			//read from index if present as it is complete
			if (fileRepo.getIndex() != null) {
				for (IndexEntry entry : fileRepo.getIndex().entries.values()) {
					if (entry.getUri().equals(uri)) {
						artifacts.add(repoCache.get(entry.getId()));
						break;
					}
				}
			} else {
				for (Entry<UUID, Artifact> entry : repoCache) {
					if (entry.getValue().getURI().equals(uri)) {
						artifacts.add(entry.getValue());
						break;
					}
				}
			}

		} else if (criteria instanceof Criteria) {
			Criteria c = (Criteria) criteria;
			if (c.getId() != null) {
				Artifact artifact = repoCache.get((UUID) criteria);
				if (artifact != null) {
					artifacts.add(artifact);
				}
			} else {

				if (fileRepo.getIndex() != null) {
					for (IndexEntry entry : fileRepo.getIndex().entries.values()) {
						if (c.getURI() != null ? entry.getUri().equals(c.getURI()) : false) {
							artifacts.add(repoCache.get(entry.getId()));
							break;
						}
						boolean matches = true;
						matches &= c.getURIPattern() != null ? entry.getUri().toString().startsWith(c.getURIPattern()) : true;
						matches &= c.getCollection() != null ? entry.getCollection().equals(c.getCollection()) : true;
						matches &= c.getVersion() != null ? entry.getVersion().equals(c.getVersion()) : true;
						matches &= c.getContentType() != null ? entry.getContentType().equals(c.getContentType()) : true;
						if (matches) {
							artifacts.add(repoCache.get(entry.getId()));
						}
					}
				} else {
					for (Entry<UUID, Artifact> entry : repoCache) {
						if (c.getURI() != null ? entry.getValue().getURI().equals(c.getURI()) : false) {
							artifacts.add(repoCache.get(entry.getValue().getId()));
							break;
						}
						boolean matches = true;
						matches &= c.getURIPattern() != null ? entry.getValue().getURI().toString().startsWith(c.getURIPattern()) : true;
						matches &= c.getCollection() != null ? entry.getValue().getCollection().equals(c.getCollection()) : true;
						matches &= c.getVersion() != null ? entry.getValue().getVersion().equals(c.getVersion()) : true;
						matches &= c.getContentType() != null ? entry.getValue().getContentType().equals(c.getContentType()) : true;
						if (matches) {
							artifacts.add(repoCache.get(entry.getValue().getId()));
						}
					}
				}
			}
		}

		return artifacts;
	}

}
