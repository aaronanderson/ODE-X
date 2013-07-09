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
package org.apache.ode.di.guice.jcache;

import java.util.UUID;
import java.util.logging.Logger;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.MutableConfiguration;
import javax.cache.spi.CachingProvider;

import org.apache.ode.spi.repo.Artifact;
import org.ode.data.memory.repo.RepositoryImpl.RepoCache;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

public class JCacheModule extends AbstractModule {

	public static Logger log = Logger.getLogger(JCacheModule.class.getName());

	protected void configure() {
		CachingProvider cachingProvider = Caching.getCachingProvider();
		CacheManager cacheManager = cachingProvider.getCacheManager();

		MutableConfiguration<UUID, Artifact> config = new MutableConfiguration<>();
		config.setStoreByValue(false)
		//.setTypes(UUID.class, LocalArtifact.class)
		//.setExpiryPolicyFactory()
				.setStatisticsEnabled(true);

		Cache<UUID, Artifact> repoCache = cacheManager.configureCache("ODE-X RepoCache", config);

		bind(new TypeLiteral<Cache<UUID, Artifact>>(){}).annotatedWith(RepoCache.class).toInstance(repoCache);
		

	}

}
