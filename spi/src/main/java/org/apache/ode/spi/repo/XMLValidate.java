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
package org.apache.ode.spi.repo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.DataHandler;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

@Singleton
public class XMLValidate implements Validate {

	public static final String VALIDATE_CMD = "validate";

	DataHandler dh;

	Map<String, Set<SchemaSource>> schemaSources = new HashMap<String, Set<SchemaSource>>();
	
	private static final Logger log = Logger.getLogger(XMLValidate.class.getName());

	public synchronized void registerSchemaSource(String contentType, SchemaSource source) {
		Set<SchemaSource> ssrcs = schemaSources.get(contentType);
		if (ssrcs == null) {
			ssrcs = new HashSet<SchemaSource>();
		}
		ssrcs.add(source);
		schemaSources.put(contentType, ssrcs);
	}

	public Provider<XMLValidate> getProvider() {
		final XMLValidate ref = this;
		return new Provider<XMLValidate>() {

			@Override
			public XMLValidate get() {
				return ref;
			}

		};
	}

	@Override
	public void setCommandContext(String cmd, DataHandler dh) throws IOException {
		this.dh = dh;
	}

	@Override
	public boolean validate(StringBuilder messages) {
		try {
			SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI );
			ArrayList<Source> sourceSchemas = new ArrayList<Source>();
			Set<SchemaSource> srcs = schemaSources.get(dh.getContentType());
			if (srcs != null) {
				for (SchemaSource src : srcs) {
					for (Source s : src.getSchemaSource()) {
						sourceSchemas.add(s);
					}
				}
			}
			Source[] sources = sourceSchemas.toArray(new Source[0]);
			Schema schema = factory.newSchema(sources);
			Validator validator = schema.newValidator();
			ValidationErrorHandler er = new ValidationErrorHandler(messages);
			validator.setErrorHandler(er);
			validator.setResourceResolver(new LSResourceResolverImpl());
			validator.validate(new StreamSource(dh.getInputStream()));
			return !er.hasError();
		} catch (Exception e) {
			messages.append(e);
			return false;
		}
	}

	public static class ValidationErrorHandler implements ErrorHandler {

		StringBuilder sb;
		boolean error = false;

		public ValidationErrorHandler(StringBuilder messages) {
			this.sb = messages;
		}

		public void warning(SAXParseException e) throws SAXException {
			sb.append(String.format("line: %d column: %d warning: %s", e.getLineNumber(), e.getColumnNumber(), e.getMessage()));
		}

		public void error(SAXParseException e) throws SAXException {
			if (e.getMessage().contains("cvc-complex-type.2.4.a")){
				log.log(Level.SEVERE,"",e);
				return;
			}
			sb.append(String.format("line: %d column: %d error: %s", e.getLineNumber(), e.getColumnNumber(), e.getMessage()));
			error = true;
		}

		public void fatalError(SAXParseException e) throws SAXException {
			sb.append(String.format("line: %d column: %d fatal: %s", e.getLineNumber(), e.getColumnNumber(), e.getMessage()));
			error = true;
		}

		public String getMessages() {
			return sb.toString();
		}

		public boolean hasError() {
			return error;
		}
	}

	public static interface SchemaSource {
		Source[] getSchemaSource();
	}
	
	public static class LSResourceResolverImpl implements LSResourceResolver{

		@Override
		public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
			//System. out.format("type %s, namespaceURI %s, publicId %s, systemId %s, baseURI %s\n", type,  namespaceURI,  publicId,  systemId,  baseURI);
			return null;
		}
		
	}
}
