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

import javax.cache.Cache;
import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.inject.Singleton;
import javax.xml.namespace.QName;

import org.apache.ode.data.core.repo.RepositoryBaseImpl;
import org.apache.ode.spi.repo.Artifact;
import org.apache.ode.spi.repo.RepositoryException;

@Singleton
public class RepositoryImpl extends RepositoryBaseImpl {

	public static final String REPOSITORY_NAMESPACE = "http://ode.apache.org/data/memory/repository";

	@Inject
	@RepoCache
	Cache<EntryKey, Artifact> repoCache;

	protected Artifact loadArtifact(URI uri, String contentType, String version) throws RepositoryException {
		EntryKey key = new EntryKey(uri, contentType, version);

		Artifact artifact = repoCache.get(key);
		if (artifact != null) {
			return artifact;
		}
		throw new RepositoryException(String.format("Artifact URI %s contentType %s version %s does not exists", uri, contentType, version));
	}

	protected void createArtifact(Artifact artifact) throws RepositoryException {
		EntryKey key = new EntryKey(artifact);

		repoCache.put(key, artifact);
	}

	protected void storeArtifact(Artifact artifact) throws RepositoryException {
		EntryKey key = new EntryKey(artifact);

		repoCache.put(key, artifact);
	}

	@Override
	public void delete(URI uri, String contentType, String version) throws RepositoryException {
		EntryKey key = new EntryKey(uri, contentType, version);

		if (!repoCache.remove(key)) {
			throw new RepositoryException(String.format("Artifact URI %s contentType %s version %s does not exists", uri, contentType, version));
		}

	}

	@Override
	public boolean exists(URI uri, String contentType, String version) throws RepositoryException {
		EntryKey key = new EntryKey(uri, contentType, version);
		return repoCache.containsKey(key);
	}

	@Qualifier
	@Target({ ElementType.FIELD, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface RepoCache {

		public String value() default "";

	}

}
