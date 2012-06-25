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
package org.apache.ode.runtime.exec.modules;

import org.apache.ode.repo.ArtifactDataSourceImpl;
import org.apache.ode.repo.RepoCommandMap;
import org.apache.ode.repo.RepoFileTypeMap;
import org.apache.ode.repo.RepositoryImpl;
import org.apache.ode.runtime.exec.platform.HealthCheck;
import org.apache.ode.spi.repo.ArtifactDataSource;
import org.apache.ode.spi.repo.Repository;

import com.google.inject.AbstractModule;

public class RepoModule extends AbstractModule {
	protected void configure() {
		bind(HealthCheck.class);
		bind(RepoFileTypeMap.class);
		bind(RepoCommandMap.class);
		bind(ArtifactDataSource.class).to(ArtifactDataSourceImpl.class);
		bind(Repository.class).to(RepositoryImpl.class);
	}

}
