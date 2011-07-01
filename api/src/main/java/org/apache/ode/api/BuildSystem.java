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

import java.io.IOException;

import javax.management.MXBean;

import org.apache.ode.api.Repository.ArtifactId;

@MXBean
public interface BuildSystem {

	public static final String OBJECTNAME = "org.apache.ode:type=BuildSystem";
	public static final String BUILDPLAN_MIMETYPE = "application/ode-build-plan";

	public void build(ArtifactId artifact) throws IOException;

	public void build(byte[] contents) throws IOException;

}