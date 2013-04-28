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
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class IOBuilderDefault<I, O> implements IOBuilder<I, O> {

	private final QName inputName;
	private final Class<I> inputClass;
	private final QName outputName;
	private final Class<O> outputClass;

	public IOBuilderDefault(QName inputName, Class<I> inputClass, QName outputName, Class<O> outputClass) {
		this.inputName = inputName;
		this.inputClass = inputClass;
		this.outputName = outputName;
		this.outputClass = outputClass;
	}

	@Override
	public I newInput(Document doc, Binder<Node> binder) throws JAXBException {
		return binder.unmarshal(doc, inputClass).getValue();
	}

	@Override
	public O newOutput(Document doc, Binder<Node> binder) throws JAXBException {
		return binder.unmarshal(doc, outputClass).getValue();
	}

	@Override
	public void setInput(Document doc, I input, Binder<Node> binder) throws JAXBException {
		if (inputName != null) {
			binder.marshal(new JAXBElement<I>(inputName, inputClass, input), doc);
		} else {
			binder.marshal(input, doc);
		}
	}

	@Override
	public void setOutput(Document doc, O output, Binder<Node> binder) throws JAXBException {
		if (outputName != null) {
			binder.marshal(new JAXBElement<O>(outputName, outputClass, output), doc);
		} else {
			binder.marshal(output, doc);
		}
	}

}