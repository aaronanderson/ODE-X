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
package org.apache.ode.spi.compiler;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.ode.spi.exec.xml.Block;
import org.apache.ode.spi.exec.xml.CompilerSetting;
import org.apache.ode.spi.exec.xml.Instruction;

/**
 * Represents a singleton compiler definition for a given contentType
 * 
 */
public abstract class Unit<I extends Instruction> {

	//not sure why this can't be JAXBElement<I> but contextual emit breaks 
	final JAXBElement<I>[] elements;
	final Map<QName, CompilerSetting> settings;

	public Unit(JAXBElement<I> ...elements) { 
		this.elements = elements;
		this.settings = new HashMap<QName, CompilerSetting>();
	}

	public QName name() {
		return elements[0].getName();
	}

	public Class<I> type() {
		return (Class<I>) elements[0].getValue().getClass();
	}

	public <S extends CompilerSetting> Map<QName, S> settings() {
		return (Map<QName, S>) settings;
	}

	abstract void emit(Block block);

}
