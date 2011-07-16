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
package org.apache.ode.runtime.xsd;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.xml.namespace.QName;

import org.apache.ode.spi.compiler.Source;
import org.apache.ode.spi.compiler.WSDLContext;
import org.apache.ode.spi.compiler.XSDContext;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.w3c.dom.Element;

public class XSDContextImpl implements XSDContext {
	
	private SchemaResolver resolver;
	
	private static final Logger log = Logger.getLogger(XSDContext.class.getName());
	
	@PostConstruct
	void init() {
		 XmlSchemaCollection collection = new XmlSchemaCollection();
		 collection.setSchemaResolver(resolver);
		try {
	
		} catch (Exception e) {
			log.log(Level.SEVERE, "", e);
		}
	}

	@Override
	public QName declareXSD(Source src) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public QName declareXSD(Element src) {
		// TODO Auto-generated method stub
		return null;
	}

}