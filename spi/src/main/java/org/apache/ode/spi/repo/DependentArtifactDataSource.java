/*
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
package org.apache.ode.spi.repo;

/**
 * 
 * Represents a DataSource that is dependent upon other DataSources in order to
 * read/write to it.
 * 
 */
public interface DependentArtifactDataSource extends ArtifactDataSource {

	public void configure(ArtifactDataSource dataSource);

	public <K> void addDependency(K mappedKey, ArtifactDataSource dataSource);

	public <K> ArtifactDataSource getDependency(K mappedKey);

}
