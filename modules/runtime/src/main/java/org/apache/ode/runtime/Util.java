package org.apache.ode.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.Instance;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Base64;
import org.apache.ignite.IgniteFileSystem;
import org.apache.ignite.igfs.IgfsFile;
import org.apache.ignite.igfs.IgfsPath;
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

	public static String encodeIntId(int id, int key) {
		ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
		buffer.putInt(id);
		byte[] idArray = buffer.array();

		buffer = ByteBuffer.allocate(Integer.BYTES);
		buffer.putInt(key);
		byte[] keyArray = buffer.array();

		// XOR encrypt
		for (int i = 0; i < idArray.length; i++) {
			idArray[i] = (byte) (idArray[i] ^ keyArray[i]);
		}
		Base32 base32 = new Base32();
		return new String(base32.encode(idArray));

	}

	public static String encodeLongId(long id, long key) {
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.putLong(id);
		byte[] idArray = buffer.array();

		buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.putLong(key);
		byte[] keyArray = buffer.array();

		// XOR encrypt
		for (int i = 0; i < idArray.length; i++) {
			idArray[i] = (byte) (idArray[i] ^ keyArray[i]);
		}

		return new String(Base64.encodeBase64URLSafeString(idArray));

	}

//	public static void main(String[] args) {
//		long longKey = new Random().nextLong();
//		System.out.format("Encode: %d %d %s\n", 1, longKey, encodeLongId(1, longKey));
//		System.out.format("Encode: %d %d %s\n", 5000, longKey, encodeLongId(5000, longKey));
//		int intKey = new Random().nextInt();
//		System.out.format("Encode: %d %d %s\n", 1, intKey, encodeIntId(1, intKey));
//		System.out.format("Encode: %d %d %s\n", 5000, intKey, encodeIntId(5000, intKey));
//	}

	public static <I, K> void index(Instance<I> instances, Map<K, List<Invocation<I>>> methodCache, MethodIndexer<I, K> methodIndexer) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		for (I instance : instances) {
			Util.index(instance, instance.getClass(), methodCache, methodIndexer);
		}
	}

	public static <I, K> void index(I instance, Class<?> clazz, Map<K, List<Invocation<I>>> methodCache, MethodIndexer<I, K> methodIndexer) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if (!Object.class.equals(clazz)) {
			for (Method method : clazz.getDeclaredMethods()) {
				methodIndexer.index(instance, method, methodCache);
			}
			if (null != clazz.getSuperclass()) {
				index(instance, clazz.getSuperclass(), methodCache, methodIndexer);
			}
		}
	}

	public static class Invocation<I> {
		private final I instance;
		private final Method method;
		private final int priority;

		public Invocation(I instance, Method method) {
			this(instance, method, 10);
		}

		public Invocation(I instance, Method method, int priority) {
			this.instance = instance;
			this.method = method;
			this.priority = priority;
		}

		public I getInstance() {
			return instance;
		}

		public Method getMethod() {
			return method;
		}

	}

	@FunctionalInterface
	public static interface MethodIndexer<I, K> {

		public void index(I instance, Method method, Map<K, List<Invocation<I>>> methodCache) throws IllegalArgumentException;

	}

	public static <I, K, R> R invoke(K key, Map<K, List<Invocation<I>>> methodCache, ParameterInitializer paramInitializer) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		List<Invocation<I>> invocations = methodCache.get(key);
		if (invocations != null) {
			for (Invocation<I> invocation : invocations) {
				Object[] args = new Object[invocation.getMethod().getParameterCount()];
				paramInitializer.initialize(args, invocation.getMethod().getParameterTypes());
				Object returnValue = invocation.getMethod().invoke(invocation.getInstance(), args);
				if (returnValue != null) {
					return (R) returnValue;
				}

			}
		}
		return null;

	}

	public static <I, R> R invoke(I instance, Class<?> clazz, InvocationFilter filter, ParameterInitializer paramInitializer) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if (!Object.class.equals(clazz)) {
			for (Method method : clazz.getDeclaredMethods()) {
				if (filter.matches(method)) {
					Object result = invoke(instance, method, paramInitializer);
					if (result != null) {
						return (R) result;
					}
				}
			}
			if (null != clazz.getSuperclass()) {
				Object result = invoke(instance, clazz.getSuperclass(), filter, paramInitializer);
				if (result != null) {
					return (R) result;
				}
			}
		}
		return null;
	}

	@FunctionalInterface
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
