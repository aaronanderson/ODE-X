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

import javax.xml.namespace.QName;

import org.apache.ode.spi.exec.xml.Instruction;
import org.apache.ode.spi.xml.HandlerRegistry;

public interface ParserRegistry<K>
		extends
		HandlerRegistry<Unit<? extends Instruction>, CompilerContext, ElementParser<Unit<? extends Instruction>>, AttributeParser<Unit<? extends Instruction>>, K> {

	@Override
	public void register(QName qname, K modelkey, ElementParser<Unit<? extends Instruction>> handler);

	@Override
	public void unregister(QName qname, K modelkey);

	@Override
	public ElementParser<Unit<? extends Instruction>> retrieve(QName qname, Unit model);

	@Override
	public void register(QName ename, QName aname, K modelkey, AttributeParser<Unit<? extends Instruction>> handler);

	@Override
	public void unregister(QName ename, QName aname, K modelkey);

	@Override
	public AttributeParser<Unit<? extends Instruction>> retrieve(QName ename, QName aname, Unit model);

}
