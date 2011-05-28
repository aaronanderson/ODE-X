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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.inject.Provider;
import javax.xml.namespace.QName;

import org.apache.ode.spi.compiler.Compiler;
import org.apache.ode.spi.compiler.CompilerPass;
import org.apache.ode.spi.compiler.CompilerPhase;

public class CompilerImpl implements Compiler {

	private Map<CompilerPhase, List<CompilerPass>> passes = new ConcurrentHashMap<CompilerPhase, List<CompilerPass>>();
	private Map<String, Provider<?>> contexts = new ConcurrentHashMap<String, Provider<?>>();
	private Set<QName> instructionSets = new CopyOnWriteArraySet<QName>();

	@Override
	public void addCompilerPass(CompilerPhase phase, CompilerPass pass) {
		List<CompilerPass> passList = passes.get(phase);
		if (passList == null) {
			passList = new ArrayList<CompilerPass>();
			passes.put(phase, passList);
		}
		passList.add(pass);
	}

	public List<CompilerPass> getCompilerPasses(CompilerPhase phase) {
		List<CompilerPass> passList = passes.get(phase);
		if (passList == null) {
			return Collections.EMPTY_LIST;
		}
		return passList;
	}

	@Override
	public <C> void addSubContext(String id, Provider<C> type) {
		contexts.put(id, type);
	}

	public Map<String, Provider<?>> getSubContexts() {
		return contexts;
	}

	@Override
	public void addInstructionSet(QName instructionSet) {
		instructionSets.add(instructionSet);
	}

	public Set<QName> getInstructionSets() {
		return instructionSets;
	}

}
