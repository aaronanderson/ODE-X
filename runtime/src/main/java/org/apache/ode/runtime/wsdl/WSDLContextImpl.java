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
package org.apache.ode.runtime.wsdl;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLLocator;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;

import org.apache.ode.runtime.xsd.XSDContextImpl;
import org.apache.ode.spi.compiler.Source;
import org.apache.ode.spi.compiler.WSDLContext;
import org.apache.ode.spi.compiler.XSDContext;
import org.w3c.dom.Element;

public class WSDLContextImpl implements WSDLContext {

	private static final Logger log = Logger.getLogger(WSDLContext.class.getName());
	private WSDLFactory factory;
	private ExtensionRegistry extensions;
	private Map<QName,byte[]> wsdls;
	
	@PostConstruct
	void init() {
		wsdls = new HashMap<QName,byte[]>();
		try {
			factory = WSDLFactory.newInstance();
			extensions = factory.newPopulatedExtensionRegistry();
		} catch (WSDLException e) {
			log.log(Level.SEVERE, "", e);
		}
	}

	@Override
	public ExtensionRegistry getExtensionRegistry() {
		return extensions;
	}

	@Override
	public WSDLReader createWSDLReader() {
		WSDLReader reader = factory.newWSDLReader();
		reader.setFeature("javax.wsdl.verbose", log.isLoggable(Level.FINE));
		reader.setExtensionRegistry(extensions);
		return reader;
	}
	

	@Override
	public QName declareWSDL(Source src) {
		wsdls.put(src.getQName(), src.getContent());
		return src.getQName();
	}

	@Override
	public QName declareWSDL(Element src) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WSDLLocator getWSDLLocator(QName src, XSDContext ctx) {
		return new WSDLLocatorImpl(wsdls.get(src),this,(XSDContextImpl)ctx);
	}

}
