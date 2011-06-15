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
package org.apache.ode.api;

import java.beans.ConstructorProperties;
import java.io.IOException;

import javax.management.MXBean;

import org.apache.ode.api.Repository.ArtifactId;

@MXBean
public interface Platform {

	public static final String OBJECTNAME = "org.apache.ode:type=Platform";
	public static final String EXEC_MIMETYPE = "application/ode-executable";

	public byte[] setup(ArtifactId executable) throws IOException;

	public void install(String id, ArtifactId executable, byte[] installData, String[] targets) throws IOException;

	public Program programInfo(String id) throws IOException;

	public Process start(String id, String[] targets) throws IOException;

	public void stop(String id, String[] targets) throws IOException;

	public void uninstall(String id, String[] targets) throws IOException;

	public static class Program {

		@ConstructorProperties({ "id" })
		public Program(String id) {
			this.id = id;
		}

		private String id;

		public String getId() {
			return id;
		}

		/*
		 * public void setId(String id) { this.id = id; }
		 * 
		 * String id(); boolean installed(); Date installDate(); Artifact
		 * executable(); byte[] installData();
		 */

	}

	public static class Process {

		@ConstructorProperties({ "id" })
		public Process(String id) {
			this.id = id;
		}

		private String id;

		public String getId() {
			return id;
		}

	}

	public static class InstanceID {

		@ConstructorProperties({ "name" })
		public InstanceID(String name) {
			this.name = name;
		}

		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

}
