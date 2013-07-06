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
package org.apache.ode.di.guice.memory;

import java.util.logging.Logger;

import org.apache.ode.data.core.repo.ArtifactDataSourceImpl;
import org.apache.ode.data.core.repo.RepoCommandMap;
import org.apache.ode.data.core.repo.RepoFileTypeMap;
import org.apache.ode.spi.repo.ArtifactDataSource;
import org.apache.ode.spi.repo.Repository;
import org.ode.data.memory.repo.FileRepository;
import org.ode.data.memory.repo.RepositoryImpl;

import com.google.inject.AbstractModule;

public class MemoryRepoModule extends AbstractModule {

	public static Logger log = Logger.getLogger(MemoryRepoModule.class.getName());

	protected void configure() {
		bind(ArtifactDataSource.class).to(ArtifactDataSourceImpl.class);
		bind(Repository.class).to(RepositoryImpl.class);
		bind(RepoCommandMap.class);
		bind(RepoFileTypeMap.class);
		bind(FileRepository.class);
	}

}
