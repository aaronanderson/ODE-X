package org.apache.ode.spi.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import javax.enterprise.inject.Vetoed;

@Vetoed
public class MapConfig implements Config {

	private Map<String, Object> mapConfig;

	public MapConfig() {
		this.mapConfig = new HashMap<>();
	}

	public MapConfig(Map<String, Object> mapConfig) {
		this.mapConfig = mapConfig;
	}

	public Map<String, Object> getMapConfig() {
		return mapConfig;
	}

	public void setMapConfig(Map<String, Object> mapConfig) {
		this.mapConfig = mapConfig;
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
	public Optional<Boolean> getBool(String path) {
		return evaluatePath(path, Boolean.class);
	}

	@Override
	public Config bool(String path, Consumer<Boolean> consumer) {
		getBool(path).ifPresent(consumer);
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
		if (mapConfig == null) {
			throw new IllegalStateException("mapConfig not set");
		}
		Object target = mapConfig;
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
			return Optional.of((T) new MapConfig((Map<String, Object>) target));
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

	@Override
	public <T> Config set(String path, T instance) {
		evaluatePath(path, instance);
		return this;

	}

	private <T> void evaluatePath(String path, T instance) {
		if (mapConfig == null) {
			throw new IllegalStateException("mapConfig not set");
		}
		Map previousTarget = null;
		Object target = mapConfig;
		LinkedList<String> traversal = new LinkedList(Arrays.asList(path.split("\\.")));

		while (!traversal.isEmpty()) {
			String currentTraversal = traversal.poll();
			previousTarget = (Map) target;
			target = previousTarget.get(currentTraversal);
			// todo support brackets for list index. Also consider using JEXL
			if (target instanceof Map) {
				target = previousTarget.get(currentTraversal);
			} else if (target == null && traversal.size() > 0) { // autocreate
				target = new HashMap<>();
				previousTarget.put(currentTraversal, target);
			} else if (traversal.isEmpty()) {
				if (instance instanceof MapConfig) {
					previousTarget.put(currentTraversal, ((MapConfig) instance).getMapConfig());
				} else {
					previousTarget.put(currentTraversal, instance);
				}

			} else if (target != null) {
				throw new IllegalArgumentException(String.format("Incompatible types %s %s %s", path, instance.getClass(), target.getClass()));
			}

		}

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
