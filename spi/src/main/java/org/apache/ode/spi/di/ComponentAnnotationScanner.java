package org.apache.ode.spi.di;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Qualifier;

import org.apache.ode.spi.di.ComponentAnnotationScanner.ComponentModel;
import org.apache.ode.spi.runtime.Component;
import org.apache.ode.spi.runtime.Component.EventSets;
import org.apache.ode.spi.runtime.Component.ExecutableSets;
import org.apache.ode.spi.runtime.Component.ExecutionConfigSets;
import org.apache.ode.spi.runtime.Component.ExecutionContextSets;
import org.apache.ode.spi.runtime.Component.Offline;
import org.apache.ode.spi.runtime.Component.Online;

public class ComponentAnnotationScanner implements AnnotationScanner<ComponentModel> {
	protected static final Logger log = Logger.getLogger(ComponentAnnotationScanner.class.getName());

	public ComponentModel scan(Class<?> clazz) {
		log.fine(String.format("scanned class %s\n", clazz));
		if (clazz.isAnnotationPresent(Component.class)) {
			ComponentModel cm = new ComponentModel();
			cm.targetClass = clazz;
			Component component = clazz.getAnnotation(Component.class);
			if (!"".equals(component.value())) {
				cm.name = component.value();
			} else {
				cm.name = clazz.getName();
				if (cm.name.indexOf('.') > -1) {
					cm.name = cm.name.substring(cm.name.lastIndexOf('.') + 1);
				}
				if (cm.name.indexOf('$') > -1) {
					cm.name = cm.name.substring(cm.name.lastIndexOf('$') + 1);
				}
			}
			//TODO scan super class
			for (Method m : clazz.getMethods()) {
				try {
					if (m.isAnnotationPresent(ExecutableSets.class)) {
						cm.executableSets = MethodHandles.lookup().unreflect(m);
					} else if (m.isAnnotationPresent(ExecutionContextSets.class)) {
						cm.executionContextSets = MethodHandles.lookup().unreflect(m);
					} else if (m.isAnnotationPresent(EventSets.class)) {
						cm.eventSets = MethodHandles.lookup().unreflect(m);
					} else if (m.isAnnotationPresent(ExecutionConfigSets.class)) {
						cm.executionConfigSets = MethodHandles.lookup().unreflect(m);
					} else if (m.isAnnotationPresent(Online.class)) {
						cm.online = MethodHandles.lookup().unreflect(m);
					} else if (m.isAnnotationPresent(Offline.class)) {
						cm.offline = MethodHandles.lookup().unreflect(m);
					}
				} catch (IllegalAccessException iae) {
					log.log(Level.SEVERE, "", iae);
					continue;
				}
			}
			return cm;
		} else {
			return null;
		}
	}

	@Qualifier
	@Retention(RUNTIME)
	@Target(FIELD)
	public @interface Components {

	}

	public static class ComponentModel {

		Class<?> targetClass;
		String name;
		MethodHandle executableSets;
		MethodHandle executionContextSets;

		MethodHandle eventSets;
		MethodHandle executionConfigSets;
		MethodHandle online;
		MethodHandle offline;

		public Class<?> getTargetClass() {
			return targetClass;
		}

		public String getName() {
			return name;
		}

		public MethodHandle getExecutableSets() {
			return executableSets;
		}

		public MethodHandle getExecutionContextSets() {
			return executionContextSets;
		}

		public MethodHandle getEventSets() {
			return eventSets;
		}

		public MethodHandle getExecutionConfigSets() {
			return executionConfigSets;
		}

		public MethodHandle getOnline() {
			return online;
		}

		public MethodHandle getOffline() {
			return offline;
		}

	}

}
