package org.apache.ode.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.ignite.IgniteFileSystem;
import org.apache.ignite.igfs.IgfsFile;
import org.apache.ignite.igfs.IgfsPath;
import org.apache.lucene.search.PointInSetQuery.Stream;
import org.apache.ode.spi.config.MapConfig;
import org.apache.ode.spi.deployment.Deployment.Entry;
import org.snakeyaml.engine.v1.api.Dump;
import org.snakeyaml.engine.v1.api.DumpSettings;
import org.snakeyaml.engine.v1.api.DumpSettingsBuilder;
import org.snakeyaml.engine.v1.api.Load;
import org.snakeyaml.engine.v1.api.LoadSettings;
import org.snakeyaml.engine.v1.api.LoadSettingsBuilder;
import org.snakeyaml.engine.v1.api.StreamDataWriter;

public class Util {

	public static <I, R> R invoke(I instance, Class<?> clazz, InvocationFilter filter, ParameterInitializer paramInitializer) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if (!Object.class.equals(clazz)) {
			for (Method method : clazz.getDeclaredMethods()) {
				if (filter.matches(method)) {
					return invoke(instance, method, paramInitializer);
				}
			}
			if (null != clazz.getSuperclass()) {
				return invoke(instance, clazz.getSuperclass(), filter, paramInitializer);
			}
		}
		return null;
	}

	public static interface InvocationFilter {

		public boolean matches(Method method);

	}

	public static class AnnotationFilter implements InvocationFilter {

		private final Class annotationType;

		public AnnotationFilter(Class annotationType) {
			this.annotationType = annotationType;
		}

		@Override
		public boolean matches(Method method) {
			return method.getDeclaredAnnotation(annotationType) != null;
		}

	}

	public static <I, R> R invoke(I instance, Method method, ParameterInitializer paramInitializer) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Object[] args = new Object[method.getParameterCount()];
		paramInitializer.initialize(args, method.getParameterTypes());

		return (R) method.invoke(instance, args);

	}

	@FunctionalInterface
	public static interface ParameterInitializer {

		public void initialize(Object[] parameters, Class<?>[] parameterTypes);

	}

	public static void listFiles(IgniteFileSystem repositoryFS, IgfsPath baseDir, List<IgfsPathEntry> files) throws IOException {
		listFiles(repositoryFS, baseDir, files, 0);
	}

	public static void listFiles(IgniteFileSystem repositoryFS, IgfsPath baseDir, List<IgfsPathEntry> files, int level) throws IOException {
		if (repositoryFS.exists(baseDir)) {
			for (IgfsFile file : repositoryFS.listFiles(baseDir)) {
				if (file.isFile()) {
					String[] components = file.path().componentsArray();
					String adjustedPath = String.join("/", Arrays.copyOfRange(components, level, components.length));
					files.add(new IgfsPathEntry(adjustedPath, repositoryFS, file.path()));
				} else {
					listFiles(repositoryFS, file.path(), files, level);
				}

			}
		}
	}

	public static final class IgfsPathEntry extends Entry {

		public IgfsPathEntry(IgniteFileSystem repositoryFS, IgfsPath location) throws IOException {
			super(location.toString(), () -> repositoryFS.open(location));
		}

		public IgfsPathEntry(String path, IgniteFileSystem repositoryFS, IgfsPath location) throws IOException {
			super(path, () -> repositoryFS.open(location));
		}

	}

	public static MapConfig loadYAMLConfig(InputStream is) throws IOException {
		Map<String, Object> yamlFile = loadYAML(is);
		if (yamlFile != null) {
			return new MapConfig(yamlFile);
		}
		return null;
	}

	public static Map<String, Object> loadYAML(InputStream is) throws IOException {
		Map<String, Object> yamlFile = null;
		LoadSettings settings = new LoadSettingsBuilder().build();
		Load load = new Load(settings);

		for (Object o : load.loadAllFromInputStream(is)) {
			if (o instanceof Map) {
				return (Map<String, Object>) o;
			}
		}
		return null;

	}

	public static void storeYAML(Map<String, Object> yamlFile, OutputStream os) throws IOException {
		DumpSettings settings = new DumpSettingsBuilder().build();
		Dump dump = new Dump(settings);
		try {
			dump.dump(yamlFile, new YamlWriter(os));
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}
	}

	public static class YamlWriter implements StreamDataWriter {
		private OutputStreamWriter writer;

		public YamlWriter(OutputStream out) {
			writer = new OutputStreamWriter(out);
		}

		@Override
		public void write(String str) {
			try {
				writer.write(str);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		@Override
		public void write(String str, int off, int len) {
			try {
				writer.write(str, off, len);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		public String getString() {
			return writer.toString();
		}
	}

}
