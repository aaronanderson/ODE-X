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
package org.apache.ode.spi.compiler.wsdl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.ode.spi.exec.xml.Block;
import org.apache.ode.spi.exec.xml.CompilerSetting;
import org.apache.ode.spi.exec.xml.Instruction;

public abstract class Unit {
	
	final public static String WSDL_NS="http://schemas.xmlsoap.org/wsdl/";
	final QName name;
	final Map<QName, CompilerSetting> settings;
	final List<? extends Unit> extensions = new ArrayList<Unit>();


	public Unit(QName name) {
		this.name = name;
		this.settings = new HashMap<QName, CompilerSetting>();
	}

	public QName name() {
		return name;
	}

	public <S extends CompilerSetting> Map<QName, S> settings() {
		return (Map<QName, S>) settings;
	}
	
	public List<? extends Unit> extensions() {
		return extensions;
	}


}
