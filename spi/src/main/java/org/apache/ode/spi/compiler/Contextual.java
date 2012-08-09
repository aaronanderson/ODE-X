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
package org.apache.ode.spi.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.xml.bind.JAXBElement;

import org.apache.ode.spi.exec.executable.xml.Block;
import org.apache.ode.spi.exec.executable.xml.Context;
import org.apache.ode.spi.exec.executable.xml.Instruction;

/**
 * Akin to a branch node
 * 
 */
public class Contextual<C extends Context> extends Unit<C> {

	final C start;
	final C end;
	final Stack<Contextual<? extends Context>> parents;
	final List<Unit<? extends Instruction>> children;

	public Contextual(JAXBElement<C> startElement, JAXBElement<C> endElement, Contextual<? extends Context> parent) throws ParserException {
		super(startElement, endElement);
		this.start = startElement.getValue();
		this.end = endElement.getValue();
		this.parents = new Stack<Contextual<? extends Context>>();
		if (parent != null) {
			if (!(this instanceof Contextual)) {
				throw new ParserException("Parent must be Contextual");
			}
			this.parents.addAll(parent.parents());
			this.parents.push(parent);
		}
		this.children = new ArrayList<Unit<? extends Instruction>>();

	}

	public C beginContext() {
		return start;
	}

	public C endContext() {
		return end;
	}

	public Stack<Contextual<? extends Context>> parents() {
		return parents;
	}

	@Override
	public void emit(Block block) {
		block.getInstructions().add(elements[0]);
		for (Unit<? extends Instruction> u : children) {
			u.emit(block);
		}
		block.getInstructions().add(elements[1]);

	}

	public List<Unit<? extends Instruction>> children() {
		return children;
	}

}
