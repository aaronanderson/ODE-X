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
package org.apache.ode.runtime.exec.platform;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.ode.runtime.exec.JAXBRuntimeUtil;
import org.apache.ode.spi.exec.ActionTask.ActionId;
import org.apache.ode.spi.exec.ActionTask.ActionStatus;
import org.apache.ode.spi.exec.Component;
import org.apache.ode.spi.exec.Component.InstructionSet;
import org.apache.ode.spi.exec.NodeStatus;
import org.apache.ode.spi.exec.Platform;
import org.apache.ode.spi.exec.PlatformException;
import org.apache.ode.spi.exec.Process;
import org.apache.ode.spi.exec.Program;
import org.apache.ode.spi.exec.Target;
import org.apache.ode.spi.exec.xml.Executable;
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

	@PersistenceContext(unitName = "platform")
	private EntityManager pmgr;

	@Inject
	Repository repo;
	@Inject
	Provider<ArtifactDataSource> dsProvider;

	@Inject
	private Cluster cluster;

	private Map<QName, Component> components = new ConcurrentHashMap<QName, Component>();
	private Map<QName, InstructionSet> instructions = new ConcurrentHashMap<QName, InstructionSet>();
	private QName architecture;

	@Override
	public void registerComponent(Component component) {
		components.put(component.name(), component);
		for (InstructionSet is : component.instructionSets()) {
			instructions.put(is.getName(), is);
		}
		cluster.addComponent(component);
	}

	public Component getComponent(QName name) {
		return components.get(name);
	}

	@Override
	public QName architecture() {
		return architecture;
	}

	public void setArchitecture(QName architecture) {
		this.architecture = architecture;
	}

	@Override
	public Document setup(Artifact... executables) throws PlatformException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder db = factory.newDocumentBuilder();
			Document programConfiguration = db.newDocument();
			
			for (Artifact ex : executables) {
				ArtifactDataSource ds = dsProvider.get();
				ds.configure(ex);
				DataHandler dh = repo.getDataHandler(ds);
				Document exec = (Document) dh.getTransferData(XMLDataContentHandler.DOM_FLAVOR);
				NodeList list = exec.getDocumentElement().getChildNodes();
				int eCount = 0;
				for (int i = 0; i < list.getLength(); i++) {
					Node n = list.item(i);
					if (n instanceof Element) {
						Element e = (Element) n;
						if ("installation".equals(e.getLocalName())) {
							programConfiguration.appendChild(programConfiguration.adoptNode(e.cloneNode(true)));
						} else if (++eCount > 2) {// if present should be 1st or 2nd
													// child element
							return null;
						}
					}

				}
			}
			return programConfiguration;
		} catch (Exception e) {
			throw new PlatformException(e);
		}

	}

	@Override
	public void install(QName id, Document programConfiguration, Target... targets) throws PlatformException {
		// cluster.execute(action, installData,targets);
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
	public ActionId execute(QName action, Document actionInput, Target... targets) throws PlatformException {
		return cluster.execute(action, actionInput, targets);
	}

	@Override
	public ActionStatus status(ActionId actionId) throws PlatformException {
		return cluster.status(actionId);
	}

	@Override
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
		Set<InstructionSet> isetSet = new HashSet<InstructionSet>();
		for (QName iset : isets) {
			InstructionSet is = instructions.get(iset);
			if (is == null) {
				throw new JAXBException(new PlatformException("Unknown instruction set " + iset.toString()));
			}
			isetSet.add(is);
		}
		return JAXBRuntimeUtil.executableJAXBContextByPath(isetSet);
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
		Set<InstructionSet> isetSet = new HashSet<InstructionSet>();
		InstructionSets isets = executable.getInstructionSets();
		if (isets != null) {
			for (QName iset : isets.getInstructionSet()) {
				InstructionSet is = instructions.get(iset);
				if (is == null) {
					throw new JAXBException(new PlatformException("Unknown instruction set " + iset.toString()));
				}
				isetSet.add(is);
			}
		}
		return JAXBRuntimeUtil.executableJAXBContextByPath(isetSet);
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

	@Override
	public Set<NodeStatus> status() {
		return cluster.status();
	}

}
