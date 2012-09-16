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
package org.apache.ode.spi.exec.task;

import javax.xml.bind.Binder;
import javax.xml.bind.JAXBException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * This is a helper class for JAXB XML DOM unmarshalling. It is possible to
 * unmarshall XML types using different XML elements this factory class
 * facilitates the auto-unmarshalling without using type reflection which may
 * not be possible for JAXBElement values due to type type erasure.
 * */
public interface IOBuilder<I, O> {

	I newInput(Document doc, Binder<Node> binder) throws JAXBException;

	O newOutput(Document doc, Binder<Node> binder) throws JAXBException;
	
	void setInput(Document doc, I input, Binder<Node> binder) throws JAXBException;

	void setOutput(Document doc, O output, Binder<Node> binder) throws JAXBException;

}