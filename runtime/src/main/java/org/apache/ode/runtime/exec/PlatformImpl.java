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
package org.apache.ode.runtime.exec;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Singleton;
import javax.xml.namespace.QName;

import org.apache.ode.spi.exec.Platform;
import org.apache.ode.spi.exec.PlatformException;
import org.apache.ode.spi.exec.Program;
import org.apache.ode.spi.repo.Artifact;

@Singleton
public class PlatformImpl implements Platform {

	private Map<QName,String> jaxbCtxs = new ConcurrentHashMap<QName, String>();
	@Override
	public void registerInstructionSet(QName instructionSet, String jaxbPath) {
		jaxbCtxs.put(instructionSet, jaxbPath);
	}
	
	public String getJAXBPath(QName instructionSet){
		return jaxbCtxs.get(instructionSet);
	}

	@Override
	public Program install(Artifact executable) throws PlatformException {

		return null;
	}

	@Override
	public void uninstall(Program program) throws PlatformException {

	}

}
