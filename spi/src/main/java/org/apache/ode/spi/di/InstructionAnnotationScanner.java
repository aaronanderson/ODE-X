package org.apache.ode.spi.di;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.inject.Qualifier;
import javax.xml.namespace.QName;

import org.apache.ode.spi.di.InstructionAnnotationScanner.InstructionModel;
import org.apache.ode.spi.exec.executable.Instruction;
import org.apache.ode.spi.exec.executable.Instruction.Execute;

public class InstructionAnnotationScanner implements AnnotationScanner<InstructionModel> {
	protected static final Logger log = Logger.getLogger(InstructionAnnotationScanner.class.getName());

	public InstructionModel scan(Class<?> clazz) {
		log.fine(String.format("scanned class %s\n", clazz));
		if (clazz.isAnnotationPresent(Instruction.class)) {

			Instruction instruction = clazz.getAnnotation(Instruction.class);
			Map<QName, MethodHandle> instructions = new HashMap<QName, MethodHandle>();
			for (Method m : clazz.getMethods()) {
				if (m.isAnnotationPresent(Execute.class)) {
					Execute op = m.getAnnotation(Execute.class);
					
				}
			}
			return new InstructionModel(clazz, instructions);
		} else {
			return null;
		}
	}
	
	@Qualifier
	@Retention(RUNTIME)
	@Target(FIELD)
	public @interface Instructions {
		
	}

	

	public static class InstructionModel {

		private Class<?> targetClass;
		private Map<QName, MethodHandle> instructions;

		public InstructionModel(Class<?> clazz, Map<QName, MethodHandle> instructions) {
			this.targetClass = clazz;
			this.instructions = instructions;
		}

		public Class<?> targetClass() {
			return targetClass;
		}

		public Map<QName, MethodHandle> instructions() {
			return instructions;
		}
	}

}
