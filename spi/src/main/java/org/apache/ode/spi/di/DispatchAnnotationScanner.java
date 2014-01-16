package org.apache.ode.spi.di;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Qualifier;

import org.apache.ode.spi.di.DispatchAnnotationScanner.DispatcherModel;
import org.apache.ode.spi.work.Dispatcher;
import org.apache.ode.spi.work.Dispatcher.BuildDispatch;
import org.apache.ode.spi.work.Dispatcher.ExecDispatch;
import org.apache.ode.spi.work.Dispatcher.Filter;

public class DispatchAnnotationScanner implements AnnotationScanner<Set<DispatcherModel>> {
	protected static final Logger log = Logger.getLogger(DispatchAnnotationScanner.class.getName());

	public Set<DispatcherModel> scan(Class<?> clazz) {
		log.fine(String.format("scanned class %s\n", clazz));
		if (clazz.isAnnotationPresent(Dispatcher.class)) {
			HashSet<DispatcherModel> dms = new HashSet<>();
			for (Method m : clazz.getMethods()) {
				DispatcherModel dm = null;
				Filter[] filters = null;
				if (m.isAnnotationPresent(BuildDispatch.class)) {
					if (!Modifier.isStatic(m.getModifiers())) {
						log.severe(String.format("BuildDispatch in Class %s method %s, is not static, skipping", clazz.getName(), m.getName()));
						continue;
					}
					BuildDispatch disp = m.getAnnotation(BuildDispatch.class);
					dm = new BuildDispatcherModel();
					((BuildDispatcherModel) dm).singleThread = disp.singleThread();
					filters = disp.value();
				}

				if (m.isAnnotationPresent(ExecDispatch.class)) {
					if (!Modifier.isStatic(m.getModifiers())) {
						log.severe(String.format("BuildDispatch in Class %s method %s, is not static, skipping", clazz.getName(), m.getName()));
						continue;
					}
					ExecDispatch disp = m.getAnnotation(ExecDispatch.class);
					dm = new ExecDispatcherModel();
					filters = disp.value();
				}

				if (dm != null) {
					List<FilterModel> filterModels = new ArrayList<>();
					if (filters != null) {
						for (Filter f : filters) {
							String ns = "".equals(f.namespace()) ? null : f.namespace();
							String nm = "".equals(f.name()) ? null : f.name();
							filterModels.add(new FilterModel(ns, nm));
						}
						if (filterModels.size() > 0) {
							dm.filterModels = filterModels.toArray(new FilterModel[filterModels.size()]);
						} else {
							log.severe(String.format("Dispatch in Class %s method %s, has no filters, skipping", clazz.getName(), m.getName()));
							continue;
						}
					}

					try {
						dm.handle = MethodHandles.lookup().unreflect(m);
					} catch (IllegalAccessException iae) {
						log.log(Level.SEVERE, "", iae);
						continue;
					}
					dms.add(dm);
				}

			}
			return dms;
		} else {
			return null;
		}
	}

	@Qualifier
	@Retention(RUNTIME)
	@Target(FIELD)
	public @interface Dispatches {

	}

	public static class DispatcherModel {

		private FilterModel[] filterModels;
		private MethodHandle handle;

		public FilterModel[] filterModels() {
			return filterModels;
		}

		public MethodHandle handle() {
			return handle;
		}

	}

	public static class BuildDispatcherModel extends DispatcherModel {

		private boolean singleThread;

		public boolean singleThread() {
			return singleThread;
		}

	}

	public static class ExecDispatcherModel extends DispatcherModel {

	}

	public static class FilterModel {

		private final String commandNamespace;
		private final String commandLocalName;

		public FilterModel(String commandNamespace, String commandLocalName) {
			this.commandNamespace = commandNamespace;
			this.commandLocalName = commandLocalName;
		}

		public String commandNamespace() {
			return commandNamespace;
		}

		public String commandLocalName() {
			return commandLocalName;
		}

	}

}
