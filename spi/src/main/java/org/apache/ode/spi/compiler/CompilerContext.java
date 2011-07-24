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

import java.util.concurrent.locks.ReadWriteLock;

import javax.xml.bind.Binder;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.ode.spi.exec.xml.Executable;
import org.apache.ode.spi.exec.xml.Instruction;
import org.w3c.dom.Node;

/**
 * Defines contextual compilation operations For a specific content type compiler instance.
 * 
 */
public interface CompilerContext {

	CompilerPhase phase();

	Source source();

	/**
	 * Generic context shared amongst compiler instances participating in group compilation
	 * 
	 * @param id
	 * @return
	 */
	<C> C subContext(String id);

	Executable executable();

	Binder<Node> executableBinder();

	ReadWriteLock executableLock();

	void addWarning(Location location, String msg, Throwable t);

	void addError(Location location, String msg, Throwable t);

	void declareSource(String contentType, Location start, Location end);

	public <U extends Unit<? extends Instruction>> void parseContent(XMLStreamReader input, U subModel) throws XMLStreamException, ParserException;
	
	public <U extends Unit<? extends Instruction>> String [] parseAttributes(XMLStreamReader input, U subModel, String ... attrName) throws XMLStreamException, ParserException;

	void terminate();

}
