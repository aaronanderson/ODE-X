package org.apache.ode.runtime.exec;

import static org.apache.ode.spi.exec.Platform.EXEC_INSTRUCTION_SET;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.Binder;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;

import org.apache.ode.runtime.exec.platform.ScopeContext.ExecutableScopeContext;
import org.apache.ode.spi.exec.Component.InstructionSet;
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

	public static JAXBContext executableJAXBContext(Set<InstructionSet> instructionSets) throws JAXBException {
		try {
			return JAXBContext.newInstance(executableClasses(instructionSets));
		} catch (ClassNotFoundException cnf) {
			throw new JAXBException(cnf);
		}
	}
	
	public static JAXBContext executableJAXBContextByPath(Set<InstructionSet> instructionSets) throws JAXBException {
		try {
			return JAXBContext.newInstance(executablePath(instructionSets));
		} catch (ClassNotFoundException cnf) {
			throw new JAXBException(cnf);
		}
	}

	public static Class<?>[] executableClasses(Set<InstructionSet> instructionSets) throws ClassNotFoundException {
		Set<Class<?>> factoryClasses = new HashSet<Class<?>>();
		factoryClasses.add(EXEC_INSTRUCTION_SET.getJAXBExecFactory());
		for (InstructionSet is : instructionSets) {
			if (is.getJAXBExecFactory() != null) {
				factoryClasses.add(is.getJAXBExecFactory());
			} else {
				factoryClasses.add(Class.forName(is.getJAXBExecPath() + ".ObjectFactory"));
			}
		}
		return (Class<?>[]) factoryClasses.toArray(new Class<?>[factoryClasses.size()]);

	}

	public static String executablePath(Set<InstructionSet> instructionSets) throws ClassNotFoundException {
		StringBuilder paths = new StringBuilder();
		paths.append(EXEC_INSTRUCTION_SET.getJAXBExecPath());
		for (InstructionSet is : instructionSets) {
			paths.append(":");
			paths.append(is.getJAXBExecPath());
		}
		return paths.toString();
	}

	public static JAXBContext executionContextJAXBContext(Set<InstructionSet> instructionSets) throws JAXBException {
		try {
			return JAXBContext.newInstance(executionContextClasses(instructionSets));
		} catch (ClassNotFoundException cnf) {
			throw new JAXBException(cnf);
		}
	}
	
	public static JAXBContext executionContextJAXBContextByPath(Set<InstructionSet> instructionSets) throws JAXBException {
		try {
			return JAXBContext.newInstance(executionContextPath(instructionSets));
		} catch (ClassNotFoundException cnf) {
			throw new JAXBException(cnf);
		}
	}

	public static Class<?>[] executionContextClasses(Set<InstructionSet> instructionSets) throws ClassNotFoundException {
		Set<Class<?>> factoryClasses = new HashSet<Class<?>>();
		factoryClasses.add(EXEC_INSTRUCTION_SET.getJAXBExecContextFactory());
		for (InstructionSet is : instructionSets) {
			if (is.getJAXBExecContextFactory() != null) {
				factoryClasses.add(is.getJAXBExecContextFactory());
			} else {
				factoryClasses.add(Class.forName(is.getJAXBExecContextPath() + ".ObjectFactory"));
			}
		}
		return (Class<?>[]) factoryClasses.toArray(new Class<?>[factoryClasses.size()]);
	}

	public static String executionContextPath(Set<InstructionSet> instructionSets) throws ClassNotFoundException {
		StringBuilder paths = new StringBuilder();
		paths.append(EXEC_INSTRUCTION_SET.getJAXBExecContextPath());
		for (InstructionSet is : instructionSets) {
			paths.append(":");
			paths.append(is.getJAXBExecContextPath());
		}
		return paths.toString();
	}

	public static void registerExec(Unmarshaller u, Set<InstructionSet> instructionSets, ExecutableScopeContext scope) throws JAXBException {
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

	public static void registerExec(Binder<Node> b, Set<InstructionSet> instructionSets, ExecutableScopeContext scope) throws JAXBException {
		try {
			scope.begin();

			try {
				Set<Object> factories = new HashSet<Object>();
				for (Class<?> c : executableClasses(instructionSets)) {
					factories.add(scope.newInstance(c));
				}
				setObjectFactories(b, factories.toArray());
			} catch (ClassNotFoundException cnf) {
				throw new JAXBException(cnf);
			}
		} finally {
			scope.end();
		}
	}

	public static void registerExecContext(Unmarshaller u, Set<InstructionSet> instructionSets, ExecutableScopeContext scope) throws JAXBException {
		try {
			scope.begin();
			try {
				Set<Object> factories = new HashSet<Object>();
				for (Class<?> c : executionContextClasses(instructionSets)) {
					factories.add(scope.newInstance(c));
				}
				setObjectFactories(u, factories.toArray());
			} catch (ClassNotFoundException cnf) {
				throw new JAXBException(cnf);
			}
		} finally {
			scope.end();
		}
	}

	public static void registerExecContext(Binder<Node> b, Set<InstructionSet> instructionSets, ExecutableScopeContext scope) throws JAXBException {
		try {
			Set<Object> factories = new HashSet<Object>();
			for (Class<?> c : executionContextClasses(instructionSets)) {
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

}
