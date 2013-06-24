package org.apache.ode.spi.di;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.ode.spi.di.ComponentAnnotationScanner.OperationModel;
import org.apache.ode.spi.exec.platform.Operation;
import org.apache.ode.spi.exec.platform.Operation.OperationSet;

public class ComponentAnnotationScanner implements AnnotationScanner<OperationModel> {
	protected static final Logger log = Logger.getLogger(ComponentAnnotationScanner.class.getName());

	public OperationModel scan(Class<?> clazz) {
		log.fine(String.format("scanned class %s\n", clazz));
		if (clazz.isAnnotationPresent(OperationSet.class)) {

			OperationSet opset = clazz.getAnnotation(OperationSet.class);
			Map<QName, QName> commands = new HashMap<QName, QName>();
			Map<QName, MethodHandle> operations = new HashMap<QName, MethodHandle>();
			for (Method m : clazz.getMethods()) {
				if (m.isAnnotationPresent(Operation.class)) {
					Operation op = m.getAnnotation(Operation.class);
					String operationNamespace = op.namespace();
					if (operationNamespace.length() == 0) {
						operationNamespace = opset.namespace();
					}
					if (operationNamespace.length() == 0) {
						log.severe(String.format("Unable to determine operation namespace for Class %s method %s, skipping", clazz.getName(), m.getName()));
						continue;
					}
					String operationName = op.name();
					if (operationName.length() == 0) {
						operationName = m.getName();
					}
					QName operationQName = new QName(operationNamespace, operationName);
					QName commandQName = null;
					MethodHandle mHandle = null;
					try {
						mHandle = MethodHandles.lookup().unreflect(m);
					} catch (IllegalAccessException iae) {
						log.log(Level.SEVERE, "", iae);
						continue;
					}
					if (op.command().name().length() > 0) {
						String commandNamespace = op.command().namespace();
						if (commandNamespace.length() == 0) {
							commandNamespace = opset.commandNamespace();
						}
						if (commandNamespace.length() == 0) {
							log.severe(String.format("Unable to determine command namespace for Class %s method %s, skipping", clazz.getName(), m.getName()));
							continue;
						}
						commandQName = new QName(commandNamespace, op.command().name());
					}
					operations.put(operationQName, mHandle);
					if (commandQName != null) {
						commands.put(commandQName, operationQName);
					}

				}
			}
			return new OperationModel(clazz, commands, operations);
		} else {
			return null;
		}
	}

	public static class OperationModel {

		private Class<?> clazz;
		private Map<QName, QName> commands;
		private Map<QName, MethodHandle> operations;

		public OperationModel(Class<?> clazz, Map<QName, QName> commands, Map<QName, MethodHandle> operations) {
			this.clazz = clazz;
			this.commands = commands;
			this.operations = operations;
		}

		public Class<?> targetClass() {
			return targetClass();
		}

		public Map<QName, QName> commands() {
			return commands;
		}

		public Map<QName, MethodHandle> operations() {
			return operations;
		}
	}

}
