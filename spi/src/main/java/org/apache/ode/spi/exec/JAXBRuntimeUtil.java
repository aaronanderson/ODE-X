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
package org.apache.ode.spi.exec;

import static org.apache.ode.spi.exec.Platform.EVENT_SET;
import static org.apache.ode.spi.exec.Platform.EXEC_CTX_SET;
import static org.apache.ode.spi.exec.Platform.EXEC_CFG_SET;
import static org.apache.ode.spi.exec.Platform.EXEC_INSTRUCTION_SET;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.Binder;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import org.apache.ode.spi.exec.Component.EventSet;
import org.apache.ode.spi.exec.Component.ExecutableSet;
import org.apache.ode.spi.exec.Component.ExecutionConfigSet;
import org.apache.ode.spi.exec.Component.ExecutionContextSet;
import org.w3c.dom.Node;

/*
 * This utility class creates JAXBContexts but more importantly binds custom ObjectFactories to the JAXB unmarshaller
 * so that the unmarshalled object tree consists of customized actionable objects
 * 
 *  The method of associating the object factory with the unmarshaller is Oracle specific and may not work with other JAXB 
 *  providers such as eclipse Moxy. Unfortunately the JAXB Binder interface does not have a way to associate a listener with it.
 *  The alternative more standards compliant approach would be to register an unmarshaller listener and then in the beforeUnmarshal
 *  method wrap the object so that it becomes managed by the JSR 330 container using the ExecutableScopeContext wrap method. How the
 *  wrapping would occur for each specific JSR 330 container is open for further research so for now we will stick with the Oracle
 *  JAXB provider specific approach.   
 */
public class JAXBRuntimeUtil {

	public static JAXBContext executableJAXBContext(Set<ExecutableSet> instructionSets) throws JAXBException {
		try {
			return JAXBContext.newInstance(executableClasses(instructionSets));
		} catch (ClassNotFoundException cnf) {
			throw new JAXBException(cnf);
		}
	}

	public static JAXBContext executableJAXBContextByPath(Set<ExecutableSet> instructionSets) throws JAXBException {
		try {
			return JAXBContext.newInstance(executablePath(instructionSets));
		} catch (ClassNotFoundException cnf) {
			throw new JAXBException(cnf);
		}
	}

	public static Class<?>[] executableClasses(Set<ExecutableSet> instructionSets) throws ClassNotFoundException {
		Set<Class<?>> factoryClasses = new HashSet<Class<?>>();
		factoryClasses.add(EXEC_INSTRUCTION_SET.getJAXBExecFactory());
		factoryClasses.add(EVENT_SET.getJAXBEventFactory());
		for (ExecutableSet is : instructionSets) {
			if (is.getJAXBExecFactory() != null) {
				factoryClasses.add(is.getJAXBExecFactory());
			} else {
				factoryClasses.add(Class.forName(is.getJAXBExecPath() + ".ObjectFactory"));
			}
		}
		return (Class<?>[]) factoryClasses.toArray(new Class<?>[factoryClasses.size()]);

	}

	public static String executablePath(Set<ExecutableSet> instructionSets) throws ClassNotFoundException {
		StringBuilder paths = new StringBuilder();
		paths.append(EXEC_INSTRUCTION_SET.getJAXBExecPath());
		paths.append(":");
		paths.append(EVENT_SET.getJAXBEventPath());
		for (ExecutableSet is : instructionSets) {
			paths.append(":");
			paths.append(is.getJAXBExecPath());
		}
		return paths.toString();
	}

	public static JAXBContext executionContextJAXBContext(Set<ExecutableSet> instructionSets, Map<QName, ExecutionContextSet> execContextSets) throws JAXBException {
		try {
			return JAXBContext.newInstance(executionContextClasses(instructionSets, execContextSets));
		} catch (ClassNotFoundException cnf) {
			throw new JAXBException(cnf);
		}
	}

	public static JAXBContext executionContextJAXBContextByPath(Set<ExecutableSet> instructionSets, Map<QName, ExecutionContextSet> execContextSets) throws JAXBException {
		try {
			return JAXBContext.newInstance(executionContextPath(instructionSets, execContextSets));
		} catch (ClassNotFoundException cnf) {
			throw new JAXBException(cnf);
		}
	}

	public static Class<?>[] executionContextClasses(Set<ExecutableSet> instructionSets, Map<QName, ExecutionContextSet> execContextSets) throws ClassNotFoundException {
		Set<Class<?>> factoryClasses = new HashSet<Class<?>>();
		factoryClasses.add(EXEC_CTX_SET.getJAXBExecContextFactory());
		for (ExecutableSet is : instructionSets) {
			QName execContextSetName = is.getExecContextSetName();
			if (execContextSetName != null) {
				ExecutionContextSet ecs = execContextSets.get(execContextSetName);
				if (ecs == null) {
					throw new ClassNotFoundException(String.format("unable to located ExecutionContextSet %s", execContextSetName));
				}

				if (ecs.getJAXBExecContextFactory() != null) {
					factoryClasses.add(ecs.getJAXBExecContextFactory());
				} else {
					factoryClasses.add(Class.forName(ecs.getJAXBExecContextPath() + ".ObjectFactory"));
				}
			}

		}
		return (Class<?>[]) factoryClasses.toArray(new Class<?>[factoryClasses.size()]);
	}

	public static String executionContextPath(Set<ExecutableSet> execInstructionSets, Map<QName, ExecutionContextSet> execContextSets) throws ClassNotFoundException {
		StringBuilder paths = new StringBuilder();
		paths.append(EXEC_CTX_SET.getJAXBExecContextPath());
		for (ExecutableSet is : execInstructionSets) {
			QName execContextSetName = is.getExecContextSetName();
			if (execContextSetName != null) {
				paths.append(":");
				ExecutionContextSet ecs = execContextSets.get(execContextSetName);
				if (ecs == null) {
					throw new ClassNotFoundException(String.format("unable to located ExecutionContextSet %s", execContextSetName));
				}
				paths.append(ecs.getJAXBExecContextPath());
			}
		}
		return paths.toString();
	}

	public static JAXBContext eventJAXBContext(Set<ExecutableSet> instructionSets, Map<QName, EventSet> eventSets) throws JAXBException {
		try {
			return JAXBContext.newInstance(eventClasses(instructionSets, eventSets));
		} catch (ClassNotFoundException cnf) {
			throw new JAXBException(cnf);
		}
	}

	public static JAXBContext eventJAXBContextByPath(Set<ExecutableSet> instructionSets, Map<QName, EventSet> eventSets) throws JAXBException {
		try {
			return JAXBContext.newInstance(eventPath(instructionSets, eventSets));
		} catch (ClassNotFoundException cnf) {
			throw new JAXBException(cnf);
		}
	}

	public static Class<?>[] eventClasses(Set<ExecutableSet> instructionSets, Map<QName, EventSet> eventSets) throws ClassNotFoundException {
		Set<Class<?>> factoryClasses = new HashSet<Class<?>>();
		factoryClasses.add(EVENT_SET.getJAXBEventFactory());
		for (ExecutableSet is : instructionSets) {
			QName eventSetName = is.getExecContextSetName();
			if (eventSetName != null) {
				EventSet evt = eventSets.get(eventSetName);
				if (evt == null) {
					throw new ClassNotFoundException(String.format("unable to located EventSet %s", eventSetName));
				}

				if (evt.getJAXBEventFactory() != null) {
					factoryClasses.add(evt.getJAXBEventFactory());
				} else {
					factoryClasses.add(Class.forName(evt.getJAXBEventPath() + ".ObjectFactory"));
				}
			}

		}
		return (Class<?>[]) factoryClasses.toArray(new Class<?>[factoryClasses.size()]);
	}

	public static String eventPath(Set<ExecutableSet> execInstructionSets, Map<QName, EventSet> eventSets) throws ClassNotFoundException {
		StringBuilder paths = new StringBuilder();
		paths.append(EVENT_SET.getJAXBEventPath());
		for (ExecutableSet is : execInstructionSets) {
			QName eventSetName = is.getExecContextSetName();
			if (eventSetName != null) {
				paths.append(":");
				EventSet evt = eventSets.get(eventSetName);
				if (evt == null) {
					throw new ClassNotFoundException(String.format("unable to located EventSet %s", eventSetName));
				}
				paths.append(evt.getJAXBEventPath());
			}
		}
		return paths.toString();
	}

	public static JAXBContext executionConfigJAXBContext(Set<ExecutableSet> instructionSets, Map<QName, ExecutionConfigSet> programSets) throws JAXBException {
		try {
			return JAXBContext.newInstance(programClasses(instructionSets, programSets));
		} catch (ClassNotFoundException cnf) {
			throw new JAXBException(cnf);
		}
	}

	public static JAXBContext executionConfigJAXBContextByPath(Set<ExecutableSet> instructionSets, Map<QName, ExecutionConfigSet> programSets) throws JAXBException {
		try {
			return JAXBContext.newInstance(programPath(instructionSets, programSets));
		} catch (ClassNotFoundException cnf) {
			throw new JAXBException(cnf);
		}
	}

	public static Class<?>[] programClasses(Set<ExecutableSet> instructionSets, Map<QName, ExecutionConfigSet> programSets) throws ClassNotFoundException {
		Set<Class<?>> factoryClasses = new HashSet<Class<?>>();
		factoryClasses.add(EXEC_CFG_SET.getJAXBProgramFactory());
		factoryClasses.add(EVENT_SET.getJAXBEventFactory());
		for (ExecutableSet is : instructionSets) {
			QName programSetName = is.getExecContextSetName();
			if (programSetName != null) {
				ExecutionConfigSet prg = programSets.get(programSetName);
				if (prg == null) {
					throw new ClassNotFoundException(String.format("unable to located ProgramSet %s", programSetName));
				}

				if (prg.getJAXBProgramFactory() != null) {
					factoryClasses.add(prg.getJAXBProgramFactory());
				} else {
					factoryClasses.add(Class.forName(prg.getJAXBProgramPath() + ".ObjectFactory"));
				}
			}

		}
		return (Class<?>[]) factoryClasses.toArray(new Class<?>[factoryClasses.size()]);
	}

	public static String programPath(Set<ExecutableSet> execInstructionSets, Map<QName, ExecutionConfigSet> programSets) throws ClassNotFoundException {
		StringBuilder paths = new StringBuilder();
		paths.append(EXEC_CFG_SET.getJAXBProgramPath());
		paths.append(":");
		paths.append(EVENT_SET.getJAXBEventPath());
		for (ExecutableSet is : execInstructionSets) {
			QName programSetName = is.getExecContextSetName();
			if (programSetName != null) {
				paths.append(":");
				ExecutionConfigSet prg = programSets.get(programSetName);
				if (prg == null) {
					throw new ClassNotFoundException(String.format("unable to located ProgramSet %s", programSetName));
				}
				paths.append(prg.getJAXBProgramPath());
			}
		}
		return paths.toString();
	}

	public static void registerExec(Unmarshaller u, Set<ExecutableSet> instructionSets, ScopeContext scope) throws JAXBException {
		try {
			Set<Object> factories = new HashSet<Object>();
			for (Class<?> c : executableClasses(instructionSets)) {
				factories.add(scope.newInstance(c));
			}
			setObjectFactories(u, factories.toArray());
		} catch (ClassNotFoundException cnf) {
			throw new JAXBException(cnf);
		}
	}

	public static void registerExec(Binder<Node> b, Set<ExecutableSet> instructionSets, ScopeContext scope) throws JAXBException {

		try {
			Set<Object> factories = new HashSet<Object>();
			for (Class<?> c : executableClasses(instructionSets)) {
				factories.add(scope.newInstance(c));
			}
			setObjectFactories(b, factories.toArray());
		} catch (ClassNotFoundException cnf) {
			throw new JAXBException(cnf);
		}
	}

	public static void registerExecContext(Unmarshaller u, Set<ExecutableSet> instructionSets, Map<QName, ExecutionContextSet> execContextSets, ScopeContext scope)
			throws JAXBException {
		try {
			Set<Object> factories = new HashSet<Object>();
			for (Class<?> c : executionContextClasses(instructionSets, execContextSets)) {
				factories.add(scope.newInstance(c));
			}
			setObjectFactories(u, factories.toArray());
		} catch (ClassNotFoundException cnf) {
			throw new JAXBException(cnf);
		}
	}

	public static void registerExecContext(Binder<Node> b, Set<ExecutableSet> instructionSets, Map<QName, ExecutionContextSet> execContextSets, ScopeContext scope)
			throws JAXBException {
		try {
			Set<Object> factories = new HashSet<Object>();
			for (Class<?> c : executionContextClasses(instructionSets, execContextSets)) {
				factories.add(scope.newInstance(c));
			}
			setObjectFactories(b, factories.toArray());
		} catch (ClassNotFoundException cnf) {
			throw new JAXBException(cnf);
		}
	}

	public static void setObjectFactories(Unmarshaller u, Object... factories) throws PropertyException {
		try {
			u.setProperty("com.sun.xml.bind.ObjectFactory", factories);
		} catch (PropertyException ex) {
			u.setProperty("com.sun.xml.internal.bind.ObjectFactory", factories);
		}
	}

	public static void setObjectFactories(Binder<Node> b, Object... factories) throws PropertyException {
		try {
			b.setProperty("com.sun.xml.bind.ObjectFactory", factories);
		} catch (PropertyException ex) {
			b.setProperty("com.sun.xml.internal.bind.ObjectFactory", factories);
		}
	}

	public static interface ScopeContext {

		<C> C newInstance(Class<C> clazz);
	}

}
