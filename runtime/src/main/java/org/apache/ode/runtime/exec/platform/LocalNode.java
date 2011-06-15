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

import javax.inject.Singleton;

@Singleton
public class LocalNode {

   /*
		public void install(QName id, Artifact executable, Document installData) throws PlatformException {
		ProgramImpl program = new ProgramImpl();
		program.setId(id);
		program.setStatus(Status.STOPPED);
		program.setInstallDate(Calendar.getInstance());
		program.setQName(executable.getQName());
		program.setVersion(executable.getVersion());
		program.setCheckSum(executable.getCheckSum());
		if (installData == null) {
			installData = setup(executable);
		}

		Installation installConfig;
		try {
			JAXBContext ctx = getJAXBContext(new ByteArrayInputStream(executable.getContent()));
			Unmarshaller u = ctx.createUnmarshaller();
			installConfig = ((JAXBElement<Installation>) u.unmarshal(new DOMSource(installData))).getValue();
			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer tform = factory.newTransformer();
			tform.setOutputProperty(OutputKeys.INDENT, "yes");
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			tform.transform(new DOMSource(installData), new StreamResult(bos));
			program.setInstallData(bos.toByteArray());
		} catch (Exception e) {
			throw new PlatformException(e);
		}
		List<Component> installedComponents = new ArrayList<Component>();
		try {
			for (Component c : components.values()) {
				c.install(id, installConfig);
				installedComponents.add(c);
				System.out.format("Program %s installed on component component %s\n", id, c.instructionSet());
			}
		} catch (PlatformException pe) {
			// uninstall installed components in case of error
			for (Component c : installedComponents) {
				try {
					c.uninstall(id);
				} catch (PlatformException pe2) {
					pe2.printStackTrace();
				}
			}
			throw pe;
		}

		pmgr.getTransaction().begin();
		pmgr.persist(program);
		pmgr.getTransaction().commit();
	}

	

	public Process start(QName id) throws PlatformException {
		ProgramImpl program = pmgr.find(ProgramImpl.class, id.toString());
		program.setStatus(Status.STARTED);

		List<Component> startedComponents = new ArrayList<Component>();
		try {
			for (Component c : components.values()) {
				c.start(id);
				startedComponents.add(c);
				System.out.format("Program %s started on component component %s\n", id, c.instructionSet());
			}
		} catch (PlatformException pe) {
			// stop started components in case of error
			for (Component c : startedComponents) {
				try {
					c.stop(id);
				} catch (PlatformException pe2) {
					pe2.printStackTrace();
				}
			}
			throw pe;
		}

		pmgr.getTransaction().begin();
		pmgr.merge(program);
		pmgr.getTransaction().commit();
		return null;
	}

	@Override
	public void stop(QName id) throws PlatformException {
		ProgramImpl program = pmgr.find(ProgramImpl.class, id.toString());
		program.setStatus(Status.STOPPED);

		List<Component> stoppedComponents = new ArrayList<Component>(components.values());

		for (Iterator<Component> i = stoppedComponents.iterator(); i.hasNext();) {
			Component c = i.next();
			try {
				c.stop(id);
				i.remove();
				System.out.format("Program %s stopped on component component %s\n", id, c.instructionSet());
			} catch (PlatformException pe2) {
				pe2.printStackTrace();
			}
		}

		for (Component c : stoppedComponents) {
			System.out.format("Unable to stop program %s from component component %s\n", id, c.instructionSet());
		}

		pmgr.getTransaction().begin();
		pmgr.merge(program);
		pmgr.getTransaction().commit();
	}

	@Override
	public void uninstall(QName id, Target... targets) throws PlatformException {
		ProgramImpl program = pmgr.find(ProgramImpl.class, id.toString());
		List<Component> uninstalledComponents = new ArrayList<Component>(components.values());

		for (Iterator<Component> i = uninstalledComponents.iterator(); i.hasNext();) {
			Component c = i.next();
			try {
				c.uninstall(id);
				i.remove();
				System.out.format("Program %s uninstalled on component component %s\n", id, c.instructionSet());
			} catch (PlatformException pe2) {
				pe2.printStackTrace();
			}
		}

		for (Component c : uninstalledComponents) {
			System.out.format("Unable to install program %s from component component %s\n", id, c.instructionSet());
		}

		pmgr.getTransaction().begin();
		pmgr.remove(program);
		pmgr.getTransaction().commit();

	}

	@Override
	public PlatformActionId execute(Action action, Target... targets){
		
	}

	@Override
	public ActionStatus status(PlatformActionId actionId){
		
	}
	
	public void cancel(PlatformActionId actionId){
		
	}

	JAXBContext getJAXBContext(InputStream executable) throws JAXBException {
		try {
			return getJAXBContext(getInstructionSets(executable));
		} catch (PlatformException pe) {
			throw new JAXBException(pe);
		}
	}

	JAXBContext getJAXBContext(List<QName> isets) throws JAXBException {
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

	List<QName> getInstructionSets(InputStream executable) throws PlatformException {
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

	JAXBContext getJAXBContext(Executable executable) throws JAXBException {
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

	List<QName> getInstructionSets(Executable executable) throws PlatformException {
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
	}*/

}
