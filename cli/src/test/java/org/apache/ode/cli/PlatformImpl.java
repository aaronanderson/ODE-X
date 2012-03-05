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
package org.apache.ode.cli;

import java.io.IOException;

import org.apache.ode.api.Platform;
import org.apache.ode.api.Repository.ArtifactId;

public class PlatformImpl implements Platform {

	@Override
	public byte[] setup(ArtifactId executable) throws IOException {

		return null;
	}

	@Override
	public void install(String id, ArtifactId executable, byte[] installData, String[] targets) throws IOException {

	}

	@Override
	public Program programInfo(String id) throws IOException {
		return null;
	}

	@Override
	public Process start(String id, String[] targets) throws IOException {

		return null;
	}

	@Override
	public void stop(String id, String[] targets) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void uninstall(String id, String[] targets) throws IOException {
		// TODO Auto-generated method stub

	}

}
