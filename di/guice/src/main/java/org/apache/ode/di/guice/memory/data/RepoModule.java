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
package org.apache.ode.di.guice.memory.data;

import java.util.UUID;
import java.util.logging.Logger;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.EternalExpiryPolicy;
import javax.cache.spi.CachingProvider;

import org.apache.ode.data.core.repo.ArtifactDataSourceImpl;
import org.apache.ode.data.core.repo.RepoCommandMap;
import org.apache.ode.data.core.repo.RepoFileTypeMap;
import org.apache.ode.data.memory.repo.FileRepoCacheLoaderFactory;
import org.apache.ode.data.memory.repo.RepositoryImpl;
import org.apache.ode.data.memory.repo.FileRepoCacheLoaderFactory.FileRepoCacheLoader;
import org.apache.ode.data.memory.repo.FileRepoCacheWriterFactory;
import org.apache.ode.data.memory.repo.FileRepoCacheWriterFactory.FileRepoCacheWriter;
import org.apache.ode.data.memory.repo.FileRepository;
import org.apache.ode.data.memory.repo.RepositoryImpl.RepoCache;
import org.apache.ode.data.memory.repo.xml.IndexMode;
import org.apache.ode.spi.repo.Artifact;
import org.apache.ode.spi.repo.ArtifactDataSource;
import org.apache.ode.spi.repo.Repository;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;

public class RepoModule extends AbstractModule {

	public static Logger log = Logger.getLogger(RepoModule.class.getName());
	IndexMode fileRepoMode = IndexMode.NONE;
	Duration duration = Duration.FIVE_MINUTES;

	public RepoModule() {
	}

	public RepoModule withFileRepoMode(IndexMode mode) {
		this.fileRepoMode = mode;
		return this;
	}

	public RepoModule withDuration(Duration duration) {
		this.duration = duration;
		return this;
	}

	protected void configure() {
		bind(ArtifactDataSource.class).to(ArtifactDataSourceImpl.class);
		bind(Repository.class).to(RepositoryImpl.class);
		bind(RepoCommandMap.class);
		bind(RepoFileTypeMap.class);
		bind(FileRepository.class);
		bind(FileRepoCacheLoader.class);
		bind(FileRepoCacheWriter.class);

		bind(new TypeLiteral<Cache<UUID, Artifact>>() {
		}).annotatedWith(RepoCache.class).toProvider(new RepoCacheProvider(getProvider(FileRepoCacheLoader.class), getProvider(FileRepoCacheWriter.class)));

	}

	public class RepoCacheProvider implements Provider<Cache<UUID, Artifact>> {
		CacheManager cacheManager;
		MutableConfiguration<UUID, Artifact> config;

		public RepoCacheProvider(Provider<FileRepoCacheLoader> loadProvider, Provider<FileRepoCacheWriter> writeProvider) {
			CachingProvider cachingProvider = Caching.getCachingProvider();
			cacheManager = cachingProvider.getCacheManager();
			config = new MutableConfiguration<>();
			config.setStoreByValue(false);
			config.setStatisticsEnabled(true);
			if (fileRepoMode == IndexMode.NONE) {
				config.setWriteThrough(true).setCacheWriterFactory(new FileRepoCacheWriterFactory(writeProvider));
			} else {
				config.setReadThrough(true).setCacheLoaderFactory(new FileRepoCacheLoaderFactory(loadProvider));
				config.setWriteThrough(true).setCacheWriterFactory(new FileRepoCacheWriterFactory(writeProvider));
			}
			if (duration != null) {
				config.setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(duration));
			} else {
				config.setExpiryPolicyFactory(EternalExpiryPolicy.factoryOf());
			}

		}

		@Override
		public Cache<UUID, Artifact> get() {
			return cacheManager.configureCache("ODE-X RepoCache", config);
		}

	}

}
