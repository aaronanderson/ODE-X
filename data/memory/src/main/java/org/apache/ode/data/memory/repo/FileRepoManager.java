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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.UUID;
import java.util.logging.Logger;

import javax.cache.Cache;
import javax.cache.configuration.MutableConfiguration;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRegistry;

import org.apache.ode.data.memory.repo.FileRepoCacheLoaderFactory.FileRepoCacheLoader;
import org.apache.ode.data.memory.repo.FileRepoCacheWriterFactory.FileRepoCacheWriter;
import org.apache.ode.data.memory.repo.RepositoryImpl.RepoCache;
import org.apache.ode.data.memory.repo.xml.ObjectFactory;
import org.apache.ode.data.memory.repo.xml.Repository;
import org.apache.ode.spi.exec.JAXBRuntimeUtil;
import org.apache.ode.spi.repo.Artifact;
import org.apache.ode.spi.repo.RepositoryException;

public class FileRepoManager {

	private static final Logger log = Logger.getLogger(FileRepoManager.class.getName());

	@Inject
	FileRepository fileRepository;

	/*
	@Inject
	@RepoCache
	Cache<UUID, Artifact> repoCache;

	@Inject
	Provider<FileRepoCacheLoader> loadProvider;

	@Inject
	Provider<FileRepoCacheWriter> writeProvider;
	
	public void setFileRepository(FileRepository repository) throws RepositoryException {
		if (!(repoCache.getConfiguration() instanceof MutableConfiguration<?, ?>)) {
			throw new RepositoryException(String.format("Expected Configuration instance %s", repoCache.getConfiguration().getClass()));
		}
		MutableConfiguration<UUID, Artifact> config = (MutableConfiguration<UUID, Artifact>) repoCache.getConfiguration();
		config.setCacheLoaderFactory(new FileRepoCacheLoaderFactory(repository, loadProvider));
		config.setCacheWriterFactory(new FileRepoCacheWriterFactory(repository, writeProvider));
		/*
		Factory<? extends CacheLoader<UUID, Artifact>> rf = repoCache.getConfiguration().getCacheLoaderFactory();
		if (rf != null) {
			if (rf instanceof FileRepoCacheLoaderFactory) {
				((FileRepoCacheLoaderFactory) rf).setFileRepository(repository);
			} else {
				throw new RepositoryException(String.format("Expected FileRepoCacheLoaderFactory instance, not %s", rf.getClass()));
			}
		} else {
			log.fine("No CacheLoaderFactory set on RepoCache");
		}

		Factory<? extends CacheWriter<? super UUID, ? super Artifact>> wf = repoCache.getConfiguration().getCacheWriterFactory();
		if (wf != null) {
			if (wf instanceof FileRepoCacheWriterFactory) {
				((FileRepoCacheWriterFactory) wf).setFileRepository(repository);
			} else {
				throw new RepositoryException(String.format("Expected FileRepoCacheLoaderFactory instance, not %s", wf.getClass()));
			}
		} else {
			log.fine("No CacheWriterFactory set on RepoCache");
		}*

	}*/

	@XmlRegistry
	public class RepoObjectFactory extends ObjectFactory {

		@Override
		public Repository createRepository() {
			return fileRepository;
		}

	}

	// most of the time the FileRepository will be read in with other configurations and 
	//will not be read in as a standalone document.
	public FileRepository loadFileRepository(InputStream xmlFile) throws IOException {
		if (xmlFile != null) {
			try {
				JAXBContext jc = JAXBContext.newInstance("org.apache.ode.data.memory.repo.xml");
				Unmarshaller u = jc.createUnmarshaller();
				u.setListener(fileRepository.new FileRepoUnMarshaller());
				JAXBRuntimeUtil.setObjectFactories(u, new RepoObjectFactory());
				JAXBElement<FileRepository> element = (JAXBElement<FileRepository>) u.unmarshal(xmlFile);
				return element.getValue();

			} catch (JAXBException je) {
				throw new IOException(je);
			}
		} else {
			throw new IOException("InputStream is null");
		}
	}

	public FileRepository loadFileRepository(String configLocation) throws IOException {
		ClassLoader currentCL = Thread.currentThread().getContextClassLoader();
		InputStream is = currentCL.getResourceAsStream(configLocation);
		if (is != null) {
			return loadFileRepository(is);
		} else {
			throw new IOException(String.format("Unable to locate config file %s on classpath\n", configLocation));
		}
	}

	//These two methods don't make much sense since artifacts should be directly stored and not their general location mappings
	public void storeFileRepository(FileRepository repo, String configLocation) throws IOException {
		ClassLoader currentCL = Thread.currentThread().getContextClassLoader();
		URL url = currentCL.getResource(configLocation);
		if (url != null) {
			storeFileRepository(repo, new FileOutputStream(url.getFile()));
		} else {
			throw new IOException(String.format("Unable to locate config file %s on classpath\n", configLocation));
		}
	}

	public void storeFileRepository(FileRepository repo, OutputStream xmlFile) throws IOException {
		if (xmlFile != null) {
			try {
				JAXBContext jc = JAXBContext.newInstance("org.apache.ode.arch.gme.xml:org.apache.ode.data.memory.repo.xml");
				Marshaller m = jc.createMarshaller();
				m.setListener(fileRepository.new FileRepoMarshaller());
				ObjectFactory of = new ObjectFactory();
				m.marshal(of.createRepository(repo), xmlFile);

			} catch (JAXBException je) {
				throw new IOException(je);
			}
		} else {
			throw new IOException("InputStream is null");
		}
	}

}
