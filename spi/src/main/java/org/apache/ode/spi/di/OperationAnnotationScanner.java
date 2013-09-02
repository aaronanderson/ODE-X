package org.apache.ode.spi.di;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Qualifier;
import javax.xml.namespace.QName;

import org.apache.ode.spi.di.OperationAnnotationScanner.OperationModel;
import org.apache.ode.spi.work.ExecutionUnit.InBuffer;
import org.apache.ode.spi.work.ExecutionUnit.OutBuffer;
import org.apache.ode.spi.work.Operation;
import org.apache.ode.spi.work.Operation.I;
import org.apache.ode.spi.work.Operation.IO;
import org.apache.ode.spi.work.Operation.IOP;
import org.apache.ode.spi.work.Operation.IP;
import org.apache.ode.spi.work.Operation.O;
import org.apache.ode.spi.work.Operation.OP;
import org.apache.ode.spi.work.Operation.OperationSet;

public class OperationAnnotationScanner implements AnnotationScanner<Map<QName, OperationModel>> {
	protected static final Logger log = Logger.getLogger(OperationAnnotationScanner.class.getName());

	public Map<QName, OperationModel> scan(Class<?> clazz) {
		log.fine(String.format("scanned class %s\n", clazz));
		if (clazz.isAnnotationPresent(OperationSet.class)) {

			OperationSet opset = clazz.getAnnotation(OperationSet.class);
			Map<QName, OperationModel> operations = new HashMap<>();
			methodScan: for (Method m : clazz.getMethods()) {
				if (m.isAnnotationPresent(Operation.class)) {
					if (!Modifier.isStatic(m.getModifiers())) {
						log.severe(String.format("Operation in Class %s method %s, is not static, skipping", clazz.getName(), m.getName()));
						continue;
					}

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
					OperationModel om = new OperationModel(operationQName);

					if (op.command().name().length() > 0) {
						String commandNamespace = op.command().namespace();
						if (commandNamespace.length() == 0) {
							commandNamespace = opset.commandNamespace();
						}
						if (commandNamespace.length() == 0) {
							log.severe(String.format("Unable to determine command namespace for Class %s method %s, skipping", clazz.getName(), m.getName()));
							continue;
						}
						om.commandName = new QName(commandNamespace, op.command().name());
					}

					try {
						om.handle = MethodHandles.lookup().unreflect(m);
					} catch (IllegalAccessException iae) {
						log.log(Level.SEVERE, "", iae);
						continue;
					}

					if (m.getReturnType() != void.class) {
						om.hasReturn = true;
					}
					List<ParameterInfo> params = new ArrayList<>(m.getParameterTypes().length);
					Annotation[][] paramAnnotations = m.getParameterAnnotations();
					Class[] parameters = m.getParameterTypes();

					for (int i = 0; i < parameters.length; i++) {
						if (paramAnnotations[i].length == 0) {
							if (InBuffer.class.isAssignableFrom(parameters[i])) {
								om.inBuffer = parameters[i];
								om.inputCount = parameters[i].getFields().length;
								params.add(new BufferInput(parameters[i]));
								continue;
							} else if (OutBuffer.class.isAssignableFrom(parameters[i])) {
								om.outBuffer = parameters[i];
								om.outputCount = parameters[i].getFields().length;
								params.add(new BufferOutput(parameters[i]));
								continue;
							} else {
								log.severe(String.format("Parameter for Class %s method %s parameter %d, is missing mandatory annotation, skipping", clazz.getName(), m.getName(),
										i));
								continue methodScan;
							}
						}
						Annotation a = paramAnnotations[i][0];
						if (a instanceof I || a instanceof IP) {
							if (om.inBuffer != null) {
								log.severe(String.format("Parameter for Class %s method %s parameter %d, is input but method has InBuffer, skipping", clazz.getName(), m.getName(),
										i));
								continue methodScan;
							}
							if (a instanceof IP) {
								if (paramAnnotations[i].length == 2 && paramAnnotations[i][1].annotationType().isAnnotationPresent(Qualifier.class)) {
									params.add(new Input(om.inputCount++, parameters[i], true, paramAnnotations[i][1]));
								} else {
									params.add(new Input(om.inputCount++, parameters[i], true, null));
								}
							} else {
								params.add(new Input(om.inputCount++, parameters[i], false, null));
							}
						} else if (a instanceof O || a instanceof OP) {
							if (om.hasReturn) {
								log.severe(String.format("Parameter for Class %s method %s parameter %d, is output but method has return value, skipping", clazz.getName(),
										m.getName(), i));
								continue methodScan;
							}
							if (om.outBuffer != null) {
								log.severe(String.format("Parameter for Class %s method %s parameter %d, is output but method has OutBuffer, skipping", clazz.getName(),
										m.getName(), i));
								continue methodScan;
							}
							if (a instanceof OP) {
								if (paramAnnotations[i].length == 2 && paramAnnotations[i][1].annotationType().isAnnotationPresent(Qualifier.class)) {
									params.add(new Output(om.outputCount++, parameters[i], true, paramAnnotations[i][1]));
								} else {
									params.add(new Output(om.outputCount++, parameters[i], true, null));
								}
							} else {
								params.add(new Output(om.outputCount++, parameters[i], false, null));
							}
						} else if (a instanceof IO || a instanceof IOP) {
							if (om.inBuffer != null) {
								log.severe(String.format("Parameter for Class %s method %s parameter %d, is inputoutput but method has InBuffer, skipping", clazz.getName(),
										m.getName(), i));
								continue methodScan;
							}
							if (om.hasReturn) {
								log.severe(String.format("Parameter for Class %s method %s parameter %d, is inputoutput but method has return value, skipping", clazz.getName(),
										m.getName(), i));
								continue methodScan;
							}
							if (om.outBuffer != null) {
								log.severe(String.format("Parameter for Class %s method %s parameter %d, is inputoutput but method has OutBuffer, skipping", clazz.getName(),
										m.getName(), i));
								continue methodScan;
							}
							if (a instanceof IOP) {
								if (paramAnnotations[i].length == 2 && paramAnnotations[i][1].annotationType().isAnnotationPresent(Qualifier.class)) {
									params.add(new InputOutput(om.inputCount++, om.outputCount++, parameters[i], true, paramAnnotations[i][1]));
								} else {
									params.add(new InputOutput(om.inputCount++, om.outputCount++, parameters[i], true, null));
								}
							} else {
								params.add(new InputOutput(om.inputCount++, om.outputCount++, parameters[i], false, null));
							}
						} else {
							log.severe(String.format("Parameter for Class %s method %s parameter %d, is unknown, skipping", clazz.getName(), m.getName(), i));
							continue methodScan;
						}
					}
					om.parameterInfo = params.toArray(new ParameterInfo[params.size()]);
					operations.put(operationQName, om);

				}
			}
			return operations;
		} else {
			return null;
		}
	}

	@Qualifier
	@Retention(RUNTIME)
	@Target(FIELD)
	public @interface Operations {

	}

	public static class OperationModel {

		private final QName operationName;
		private QName commandName;
		private MethodHandle handle;
		private ParameterInfo[] parameterInfo;
		private int inputCount = 0;
		private int outputCount = 0;
		private boolean hasReturn = false;
		private Class<InBuffer> inBuffer = null;
		private Class<OutBuffer> outBuffer = null;

		public OperationModel(QName operationName) {
			this.operationName = operationName;
		}

		public QName commandName() {
			return commandName;
		}

		public QName operationName() {
			return operationName;
		}

		public MethodHandle handle() {
			return handle;
		}

		public ParameterInfo[] parameterInfo() {
			return parameterInfo;
		}

		public int inputCount() {
			return inputCount;
		}

		public int outputCount() {
			return outputCount;
		}

		public boolean hasReturn() {
			return hasReturn;
		}

		public Class<InBuffer> inBuffer() {
			return inBuffer;
		}

		public Class<OutBuffer> outBuffer() {
			return outBuffer;
		}

	}

	public static interface ParameterInfo {

	}

	public static class IOBase implements ParameterInfo {
		public final int index;
		public final boolean inject;
		public final Class<?> paramType;
		public final Annotation qualifier;

		public IOBase(int index, Class<?> paramType, boolean inject, Annotation qualifier) {
			this.index = index;
			this.paramType = paramType;
			this.inject = inject;
			this.qualifier = qualifier;
		}

	}

	public static class Input extends IOBase {

		public Input(int index, Class<?> paramType, boolean inject, Annotation qualifier) {
			super(index, paramType, inject, qualifier);
		}

	}

	public static class BufferInput implements ParameterInfo {
		public final Class<? extends InBuffer> buffer;

		public BufferInput(Class<? extends InBuffer> buffer) {
			this.buffer = buffer;
		}
	}

	public static class Output extends IOBase {

		public Output(int index, Class<?> paramType, boolean inject, Annotation qualifier) {
			super(index, paramType, inject, qualifier);
		}

	}

	public static class InputOutput extends IOBase {
		public final int outIndex;

		public InputOutput(int inIndex, int outIndex, Class<?> paramType, boolean inject, Annotation qualifier) {
			super(inIndex, paramType, inject, qualifier);
			this.outIndex = outIndex;
		}

	}

	public static class BufferOutput implements ParameterInfo {
		public final Class<? extends OutBuffer> buffer;

		public BufferOutput(Class<? extends OutBuffer> buffer) {
			this.buffer = buffer;
		}
	}

}
