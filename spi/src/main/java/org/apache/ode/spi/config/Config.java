package org.apache.ode.spi.config;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public interface Config {
	
	public static final String ODE_HOME = "ODE_HOME";
	public static final String ODE_BASE_DIR = "ODE_BASE_DIR";
	public static final String ODE_TENANT = "ODE_TENANT";
	public static final String ODE_CONFIG = "ODE_CONFIG";


	Optional<Config> getConfig(String path);

	Config config(String path, Consumer<Config> consumer);

	Optional<String> getString(String path);

	Config string(String path, Consumer<String> consumer);

	Optional<Integer> getNumber(String path);

	Config number(String path, Consumer<Integer> consumer);
	
	Optional<Boolean> getBool(String path);

	Config bool(String path, Consumer<Boolean> consumer);

	<T> Optional<Map<String, T>> getMap(String path, Class<T> type);

	<T> Config map(String path, Class<T> type, Consumer<Map<String, T>> consumer);

	<T> Optional<List<T>> getList(String path, Class<T> type);

	<T> Config list(String path, Class<T> type, Consumer<List<T>> consumer);

}
