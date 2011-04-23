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
package org.apache.ode.runtime.build;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.ode.spi.repo.Repository;
import org.apache.ode.spi.repo.XMLDataContentHandler;

@Singleton
public class BuildSystem {
	public static String BUILDPLAN_MIMETYPE="application/ode-build-plan";
	@Inject
	Repository repository;
	@Inject
	Provider<BuildExecutor> buildProvider;

	@PostConstruct
	public void init() {
		System.out.println("Initializing BuildExecutor");
		repository.registerExtension("plan", BUILDPLAN_MIMETYPE);
		repository.registerCommandInfo(BUILDPLAN_MIMETYPE, "build", true, buildProvider);
		try {
			JAXBContext jc = JAXBContext.newInstance("org.apache.ode.runtime.build.xml");
			repository.registerHandler(BUILDPLAN_MIMETYPE, new XMLDataContentHandler(jc));
		} catch (JAXBException je) {
			je.printStackTrace();
		}

		System.out.println("BuildExecutor Initialized");

	}

}
