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
package org.apache.ode.runtime.exec;

import java.io.InputStream;

import javax.activation.DataSource;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.ode.spi.exec.Platform;
import org.apache.ode.spi.exec.xml.Executable;
import org.apache.ode.spi.exec.xml.InstructionSet;
import org.apache.ode.spi.repo.JAXBDataContentHandler;
import org.apache.ode.spi.repo.Repository;

@Singleton
public class Exec {
	public static final String EXEC_JAXB_CTX = "org.apache.ode.spi.exec.xml";
	@Inject
	Repository repository;
	@Inject
	PlatformImpl platform;

	@PostConstruct
	public void init() {
		System.out.println("Initializing Execution Runtime");
		repository.registerFileExtension("exec", Platform.EXEC_MIMETYPE);
		repository.registerNamespace(Platform.EXEC_NAMESPACE, Platform.EXEC_MIMETYPE);
		repository.registerHandler(Platform.EXEC_MIMETYPE, new ExecutableJAXBDataContentHandler(platform));

		System.out.println("Execution Runtime Initialized");

	}

	public static class ExecutableJAXBDataContentHandler extends JAXBDataContentHandler {
		PlatformImpl platform;

		public ExecutableJAXBDataContentHandler(PlatformImpl platform) {
			this.platform = platform;
		}

		@Override
		protected JAXBContext getJAXBContext(DataSource dataSource) throws JAXBException {
			StringBuilder ctxs = new StringBuilder(EXEC_JAXB_CTX);
			try {
				InputStream is = dataSource.getInputStream();
				XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(is);
				reader.nextTag();// root
				reader.nextTag();// Instruction set, if present
				if ("InstructionSets".equals(reader.getLocalName())) {
					reader.nextTag();// first instruction set
					while ("InstructionSet".equals(reader.getLocalName())) {
						/*
						 * reader.get // reader.nextTag();//next iset QName iset
						 * = null; String jaxbPath = platform.getJAXBPath(iset);
						 * if (jaxbPath==null){ throw new
						 * JAXBException("Unknown instruction set " +
						 * iset.toString()); }
						 */
					}
				}
				return JAXBContext.newInstance(ctxs.toString());
			} catch (Exception e) {
				throw new JAXBException(e);
			}
		}

		@Override
		protected JAXBContext getJAXBContext(JAXBElement<Executable> exec) throws JAXBException {
			StringBuilder ctxs = new StringBuilder(EXEC_JAXB_CTX);
			InstructionSet isets = exec.getValue().getInstructionSet();
			if (isets != null) {
				for (QName iset : isets.getInstructionSet()) {
					String jaxbPath = platform.getJAXBPath(iset);
					if (jaxbPath == null) {
						throw new JAXBException("Unknown instruction set " + iset.toString());
					}
					ctxs.append(':');
					ctxs.append(jaxbPath);
				}
			}
			return JAXBContext.newInstance(ctxs.toString());
		}

		@Override
		public QName getDefaultQName(DataSource dataSource) {
			QName defaultName = null;
			try {
				InputStream is = dataSource.getInputStream();
				XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(is);
				reader.nextTag();
				String tns = reader.getAttributeValue(null, "targetNamespace");
				String name = reader.getAttributeValue(null, "name");
				reader.close();
				if (tns != null && name != null) {
					defaultName = new QName(tns, name);
				}
				return defaultName;
			} catch (Exception e) {
				return null;
			}
		}
	}

}
