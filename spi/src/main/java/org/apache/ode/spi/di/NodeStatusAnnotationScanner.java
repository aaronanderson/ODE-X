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

import org.apache.ode.spi.di.NodeStatusAnnotationScanner.NodeStatusModel;
import org.apache.ode.spi.exec.Node.NodeStatus;
import org.apache.ode.spi.exec.Node.Offline;
import org.apache.ode.spi.exec.Node.Online;

public class NodeStatusAnnotationScanner implements AnnotationScanner<NodeStatusModel> {
	protected static final Logger log = Logger.getLogger(NodeStatusAnnotationScanner.class.getName());

	public NodeStatusModel scan(Class<?> clazz) {
		log.fine(String.format("scanned class %s\n", clazz));
		if (clazz.isAnnotationPresent(NodeStatus.class)) {
			//TODO must be singleton, check throws only PlatformException
			NodeStatusModel ns = new NodeStatusModel(clazz);
			for (Method m : clazz.getMethods()) {
				try {
					if (m.isAnnotationPresent(Online.class)) {
						ns.online = MethodHandles.lookup().unreflect(m);
					}
					if (m.isAnnotationPresent(Offline.class)) {
						ns.offline = MethodHandles.lookup().unreflect(m);
					}
				} catch (IllegalAccessException iae) {
					log.log(Level.SEVERE, "", iae);
					continue;
				}
			}
			return ns;
		} else {
			return null;
		}
	}

	@Qualifier
	@Retention(RUNTIME)
	@Target(FIELD)
	public @interface NodeStatuses {

	}

	public static class NodeStatusModel {

		Class<?> targetClass;
		MethodHandle online;
		MethodHandle offline;

		public NodeStatusModel(Class<?> clazz) {
			this.targetClass = clazz;
		}

		public Class<?> getTargetClass() {
			return targetClass;
		}

		public MethodHandle getOnline() {
			return online;
		}

		public MethodHandle getOffline() {
			return offline;
		}

	}

}
