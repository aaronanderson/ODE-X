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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;
import java.util.UUID;

import javax.cache.Cache;
import javax.cache.Cache.Entry;
import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.inject.Singleton;

import org.apache.ode.data.core.repo.RepositoryBaseImpl;
import org.apache.ode.spi.repo.Artifact;
import org.apache.ode.spi.repo.Criteria;
import org.apache.ode.spi.repo.RepositoryException;

@Singleton
public class RepositoryImpl extends RepositoryBaseImpl {

	public static final String REPOSITORY_NAMESPACE = "http://ode.apache.org/data/memory/repository";

	@Inject
	@RepoCache
	Cache<UUID, Artifact> repoCache;

	@Override
	protected <R> Artifact loadArtifact(R criteria) throws RepositoryException {

		Artifact artifact = scan(criteria);
		if (artifact != null) {
			return artifact;
		}
		throw new RepositoryException(String.format("Artifact %s does not exists", criteria));
	}

	protected void createArtifact(Artifact artifact) throws RepositoryException {
		repoCache.put(artifact.getId(), artifact);
	}

	protected void storeArtifact(Artifact artifact) throws RepositoryException {
		repoCache.put(artifact.getId(), artifact);
	}

	@Override
	public <R> void delete(R criteria) throws RepositoryException {
		Artifact artifact = scan(criteria);

		if (artifact == null || !repoCache.remove(artifact.getId())) {
			throw new RepositoryException(String.format("Artifact %s does not exists", criteria));
		}

	}

	@Override
	public <R> boolean exists(R criteria) throws RepositoryException {
		Artifact artifact = scan(criteria);
		if (artifact != null) {
			return true;
		}
		return false;
	}

	@Qualifier
	@Target({ ElementType.FIELD, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface RepoCache {

		public String value() default "";

	}

	protected <R> Artifact scan(R criteria) throws RepositoryException {
		Artifact artifact = null;
		if (criteria instanceof UUID) {
			artifact = repoCache.get((UUID) criteria);
		} else if (criteria instanceof URI) {
			URI uri = (URI) criteria;
			for (Entry<UUID, Artifact> entry : repoCache) {
				if (entry.getValue().getURI().equals(uri)) {
					if (artifact != null) {
						throw new RepositoryException(String.format("duplicate entry for URI (%s,%s, narrow criteria", artifact.getId(), entry.getValue().getId()));
					}
					artifact = entry.getValue();
				}
			}
		} else if (criteria instanceof Criteria) {
			Criteria c = (Criteria) criteria;
			if (c.getId() != null) {
				artifact = repoCache.get((UUID) criteria);
			} else {
				for (Entry<UUID, Artifact> entry : repoCache) {
					boolean uriMatches = c.getURI() != null ? entry.getValue().getURI().equals(c.getURI()) : true;
					boolean versionMatches = c.getVersion() != null ? entry.getValue().getVersion().equals(c.getVersion()) : true;
					boolean typeMatches = c.getContentType() != null ? entry.getValue().getContentType().equals(c.getContentType()) : true;
					if (uriMatches && versionMatches && typeMatches) {
						if (artifact != null) {
							throw new RepositoryException(String.format("duplicate entry for Criteria (%s,%s, narrow criteria", artifact.getId(), entry.getValue().getId()));
						}
						artifact = entry.getValue();
					}
				}
			}
		}

		return artifact;
	}

}
