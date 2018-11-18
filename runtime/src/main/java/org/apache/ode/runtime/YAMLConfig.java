package org.apache.ode.runtime;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.ode.spi.config.Config;

public class YAMLConfig implements Config {

	private Map<String, Object> yamlConfig;

	public YAMLConfig() {
		this.yamlConfig = new HashMap<>();
	}

	public YAMLConfig(Map<String, Object> yamlConfig) {
		this.yamlConfig = yamlConfig;
	}

	public Map<String, Object> getYamlConfig() {
		return yamlConfig;
	}

	public void setYamlConfig(Map<String, Object> yamlConfig) {
		this.yamlConfig = yamlConfig;
	}

	@Override
	public Optional<Config> getConfig(String path) {
		return evaluatePath(path, Config.class);
	}

	@Override
	public Config config(String path, Consumer<Config> consumer) {
		getConfig(path).ifPresent(consumer);
		return this;
	}

	@Override
	public Optional<String> getString(String path) {
		return evaluatePath(path, String.class);
	}

	@Override
	public Config string(String path, Consumer<String> consumer) {
		getString(path).ifPresent(consumer);
		return this;
	}

	@Override
	public Optional<Integer> getNumber(String path) {
		return evaluatePath(path, Integer.class);
	}

	@Override
	public Config number(String path, Consumer<Integer> consumer) {
		getNumber(path).ifPresent(consumer);
		return this;
	}

	@Override
	public <T> Optional<Map<String, T>> getMap(String path, Class<T> type) {
		return evaluatePath(path, Map.class);
	}

	@Override
	public <T> Config map(String path, Class<T> type, Consumer<Map<String, T>> consumer) {
		getMap(path, type).ifPresent(consumer);
		return this;
	}

	@Override
	public <T> Optional<List<T>> getList(String path, Class<T> type) {
		return evaluatePath(path, List.class);
	}

	@Override
	public <T> Config list(String path, Class<T> type, Consumer<List<T>> consumer) {
		getList(path, type).ifPresent(consumer);
		return this;
	}

	private <T> Optional<T> evaluatePath(String path, Class<?> type) {
		if (yamlConfig == null) {
			throw new IllegalStateException("yamlConfig not set");
		}
		Object target = yamlConfig;
		LinkedList<String> traversal = new LinkedList(Arrays.asList(path.split("\\.")));
		while (!traversal.isEmpty()) {
			String currentTraversal = traversal.poll();
			// todo support brackets for list index. Also consider using JEXL
			if (target instanceof Map) {
				target = ((Map) target).get(currentTraversal);
			} else {
				target = null;
			}

		}
		if (target == null) {
			return Optional.empty();
		}
		if (type.isAssignableFrom(Config.class) && Map.class.isAssignableFrom(target.getClass())) {
			return Optional.of((T) new YAMLConfig((Map<String, Object>) target));
		}
		if (type.isAssignableFrom(Map.class)) {
			return Optional.of((T) Collections.unmodifiableMap((Map<String, Object>) target));
		}
		if (type.isAssignableFrom(List.class)) {
			return Optional.of((T) Collections.unmodifiableList((List<T>) target));
		}
		if (type.isAssignableFrom(target.getClass())) {
			return Optional.of((T) target);
		}

		return Optional.empty();
	}

//	public static Optional<Map<String, Object>> lookup(Optional<Map<String, Object>> entry, String... path) {
//		Map<String, Object> current = null;
//		if (entry.isPresent()) {
//			current = entry.get();
//			for (String part : path) {
//				Object currentObj = current.get(part);
//				if (currentObj != null && currentObj instanceof Map) {
//					current = (Map<String, Object>) currentObj;
//				} else {
//					current = null;
//					break;
//				}
//			}
//		}
//
//		return Optional.ofNullable(current);
//	}
//
//	public static <T> Optional<T> get(Optional<Map<String, Object>> entry, String key, Class<T> type, boolean strict) {
//		if (entry.isPresent()) {
//			Object value = entry.get().get(key);
//			if (value != null) {
//				if (type.isAssignableFrom(value.getClass())) {
//					return Optional.of((T) value);
//				} else {
//					throw new IllegalArgumentException(String.format("key %s is not of expected type of %s is unavailable", key));
//				}
//			}
//		}
//		if (strict) {
//			throw new IllegalArgumentException(String.format("key %s is unavailable", key));
//		}
//		return Optional.empty();
//
//	}
//
//	public static <T> void set(Optional<Map<String, Object>> entry, String key, Class<T> type, Consumer<T> setter, boolean strict) {
//		Optional<T> value = get(entry, key, type, strict);
//		if (value.isPresent()) {
//			setter.accept(value.get());
//		} else if (strict) {
//			throw new IllegalArgumentException(String.format("key %s is unavailable", key));
//		}
//	}

}
