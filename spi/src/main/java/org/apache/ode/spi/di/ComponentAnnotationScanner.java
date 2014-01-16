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
import org.apache.ode.spi.runtime.Component.Depend;
import org.apache.ode.spi.runtime.Component.EventSets;
import org.apache.ode.spi.runtime.Component.ExecutableSets;
import org.apache.ode.spi.runtime.Component.ExecutionConfigSets;
import org.apache.ode.spi.runtime.Component.ExecutionContextSets;
import org.apache.ode.spi.runtime.Component.Offline;
import org.apache.ode.spi.runtime.Component.Online;
import org.apache.ode.spi.runtime.Component.Start;
import org.apache.ode.spi.runtime.Component.Stop;

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
			if (component.depends().length > 0) {
				cm.depends = new String[component.depends().length];
				for (int i = 0; i < component.depends().length; i++) {
					cm.depends[i] = component.depends()[i].value();
					if (cm.depends[i].length()==0){
						log.severe(String.format("Depend annotation in Component annotation on class %s is empty, skipping component", clazz));
						return null;
					}
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
					} else if (m.isAnnotationPresent(Start.class)) {
						cm.start = MethodHandles.lookup().unreflect(m);
					} else if (m.isAnnotationPresent(Online.class)) {
						cm.online = MethodHandles.lookup().unreflect(m);
					} else if (m.isAnnotationPresent(Stop.class)) {
						cm.stop = MethodHandles.lookup().unreflect(m);
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
		String[] depends;
		MethodHandle executableSets;
		MethodHandle executionContextSets;

		MethodHandle eventSets;
		MethodHandle executionConfigSets;
		MethodHandle start;
		MethodHandle online;
		MethodHandle stop;
		MethodHandle offline;

		public Class<?> getTargetClass() {
			return targetClass;
		}

		public String getName() {
			return name;
		}

		public String[] getDepends() {
			return depends;
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

		public MethodHandle getStart() {
			return start;
		}

		public MethodHandle getOnline() {
			return online;
		}

		public MethodHandle getStop() {
			return stop;
		}

		public MethodHandle getOffline() {
			return offline;
		}

	}

}
