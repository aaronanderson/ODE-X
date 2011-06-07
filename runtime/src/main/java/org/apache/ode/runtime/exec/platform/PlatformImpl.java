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
package org.apache.ode.runtime.exec.platform;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.ode.spi.exec.Action;
import org.apache.ode.spi.exec.ActionStatus;
import org.apache.ode.spi.exec.Component;
import org.apache.ode.spi.exec.Platform;
import org.apache.ode.spi.exec.PlatformException;
import org.apache.ode.spi.exec.Process;
import org.apache.ode.spi.exec.Program;
import org.apache.ode.spi.exec.Action.ActionId;
import org.apache.ode.spi.exec.Program.Status;
import org.apache.ode.spi.exec.Target;
import org.apache.ode.spi.exec.xml.Executable;
import org.apache.ode.spi.exec.xml.Installation;
import org.apache.ode.spi.exec.xml.InstructionSets;
import org.apache.ode.spi.repo.Artifact;
import org.apache.ode.spi.repo.ArtifactDataSource;
import org.apache.ode.spi.repo.DataHandler;
import org.apache.ode.spi.repo.Repository;
import org.apache.ode.spi.repo.XMLDataContentHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Singleton
public class PlatformImpl implements Platform {

	public static final String EXEC_JAXB_CTX = "org.apache.ode.spi.exec.xml";

	@PersistenceContext(unitName = "platform")
	private EntityManager pmgr;

	@Inject
	Repository repo;
	@Inject
	Provider<ArtifactDataSource> dsProvider;

	private Cluster cluster;
	private Map<QName, Component> components = new ConcurrentHashMap<QName, Component>();

	@Override
	public void registerComponent(Component component) {
		components.put(component.instructionSet(), component);
	}

	public Component getComponent(QName instructionSet) {
		return components.get(instructionSet);
	}

	@Override
	public Document setup(Artifact executable) throws PlatformException {
		try {
			ArtifactDataSource ds = dsProvider.get();
			ds.configure(executable);
			DataHandler dh = repo.getDataHandler(ds);
			Document exec = (Document) dh.getTransferData(XMLDataContentHandler.DOM_FLAVOR);
			NodeList list = exec.getDocumentElement().getChildNodes();
			int eCount = 0;
			for (int i = 0; i < list.getLength(); i++) {
				Node n = list.item(i);
				if (n instanceof Element) {
					Element e = (Element) n;
					if ("installation".equals(e.getLocalName())) {
						DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
						factory.setNamespaceAware(true);
						DocumentBuilder db = factory.newDocumentBuilder();
						Document instData = db.newDocument();
						instData.appendChild(instData.adoptNode(e.cloneNode(true)));
						return instData;
					} else if (++eCount > 2) {// if present should be 1st or 2nd
												// child element
						return null;
					}
				}

			}
			return null;
		} catch (Exception e) {
			throw new PlatformException(e);
		}

	}

	@Override
	public void install(QName id, Artifact executable, Document installData, Target... targets) throws PlatformException {
	  //cluster.execute(action, installData,targets);
	}

	@Override
	public Program programInfo(QName id) throws PlatformException {
		return pmgr.find(ProgramImpl.class, id.toString());

	}

	@Override
	public Process start(QName id, Target... targets) throws PlatformException {
	  return null;
	}

	@Override
	public void stop(QName id, Target... targets) throws PlatformException {
	}

	@Override
	public void uninstall(QName id, Target... targets) throws PlatformException {

	}

	@Override
	public ActionId execute(Action action, Document actionInfo, Target... targets) throws PlatformException {
		return cluster.execute(action, actionInfo, targets);
	}

	@Override
	public ActionStatus status(ActionId actionId) throws PlatformException {
		return cluster.status(actionId);
	}

	public void cancel(ActionId actionId) throws PlatformException {
		cluster.cancel(actionId);
	}

	public JAXBContext getJAXBContext(InputStream executable) throws JAXBException {
		try {
			return getJAXBContext(getInstructionSets(executable));
		} catch (PlatformException pe) {
			throw new JAXBException(pe);
		}
	}

	public JAXBContext getJAXBContext(List<QName> isets) throws JAXBException {
		StringBuilder ctxs = new StringBuilder(EXEC_JAXB_CTX);
		try {
			for (QName iset : isets) {
				Component c = components.get(iset);
				if (c == null) {
					throw new PlatformException("Unknown instruction set " + iset.toString());
				}
				ctxs.append(':');
				ctxs.append(c.jaxbContextPath());
			}
			return JAXBContext.newInstance(ctxs.toString());
		} catch (Exception e) {
			throw new JAXBException(e);
		}
	}

	public List<QName> getInstructionSets(InputStream executable) throws PlatformException {
		List<QName> isets = new ArrayList<QName>();
		try {
			XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(executable);
			reader.nextTag();// root
			reader.nextTag();// Instruction set, if present
			if ("instruction-sets".equals(reader.getLocalName())) {
				reader.nextTag();// first instruction set
				while ("instruction-set".equals(reader.getLocalName())) {
					String[] text = reader.getElementText().split(":");
					if (text.length == 2) {
						QName iset = new QName(reader.getNamespaceContext().getNamespaceURI(text[0]), text[1]);
						Component c = components.get(iset);
						if (c == null) {
							throw new PlatformException("Unknown instruction set " + iset.toString());
						}
						isets.add(iset);
					} else {
						throw new PlatformException("Unknown instruction set " + reader.getElementText());
					}
					reader.nextTag();
				}
			}
			executable.close();
			return isets;
		} catch (Exception e) {
			throw new PlatformException(e);
		}
	}

	public JAXBContext getJAXBContext(Executable executable) throws JAXBException {
		StringBuilder ctxs = new StringBuilder(EXEC_JAXB_CTX);
		InstructionSets isets = executable.getInstructionSets();
		if (isets != null) {
			for (QName iset : isets.getInstructionSet()) {
				Component c = components.get(iset);
				if (c == null) {
					throw new JAXBException("Unknown instruction set " + iset.toString());
				}
				ctxs.append(':');
				ctxs.append(c.jaxbContextPath());
			}
		}
		return JAXBContext.newInstance(ctxs.toString());
	}

	public List<QName> getInstructionSets(Executable executable) throws PlatformException {
		List<QName> isets = new ArrayList<QName>();
		InstructionSets eisets = executable.getInstructionSets();
		if (eisets != null) {
			for (QName iset : eisets.getInstructionSet()) {
				Component c = components.get(iset);
				if (c == null) {
					throw new PlatformException("Unknown instruction set " + iset.toString());
				}
				isets.add(iset);
			}
		}
		return isets;
	}

}
