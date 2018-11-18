package org.apache.ode.runtime;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Priority;

import org.apache.ignite.IgniteSystemProperties;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.ode.spi.config.Config;
import org.apache.ode.spi.config.IgniteConfigurator;
import org.snakeyaml.engine.v1.api.Load;
import org.snakeyaml.engine.v1.api.LoadSettings;
import org.snakeyaml.engine.v1.api.LoadSettingsBuilder;

@Priority(value = 1)
public class Configurator implements IgniteConfigurator {

	public static final String ODE_HOME = "ODE_HOME";
	public static final String ODE_BASE_DIR = "ODE_BASE_DIR";
	public static final String ODE_TENANT = "ODE_TENANT";
	public static final String ODE_CONFIG = "ODE_CONFIG";

	public static final Logger LOG = LogManager.getLogger(Configurator.class);

	public static String getProperty(String name, String defaultValue) {
		String value = System.getenv().get(name);
		if (value == null) {
			value = System.getProperty(name);
		}
		if (value == null) {
			value = defaultValue;
		}
		return value;
	}

	public static Path getODEHome() {
		String odeHome = getProperty(ODE_HOME, null);
		if (odeHome != null) {
			return Paths.get(odeHome).toAbsolutePath();
		}
		return Paths.get(System.getProperty("user.dir")).resolve("ode").toAbsolutePath();
	}

	public static String getODETenant() {
		String odeTenant = getProperty(ODE_TENANT, null);
		if (odeTenant != null) {
			return odeTenant;
		}
		return "default";
	}

	public static Config loadConfigFile(String configFile) {
		URL odeConfigURL = null;
		String odeConfig = configFile;
		if (odeConfig == null) {
			odeConfig = getProperty(ODE_CONFIG, null);
		}
		if (odeConfig == null) {
			odeConfig = "ode.yml";
		}

		Path odeConfigFile = Paths.get(odeConfig);
		if (!odeConfigFile.isAbsolute()) {
			odeConfigFile = getODEHome().resolve(odeConfig);
		}
		if (Files.exists(odeConfigFile)) {
			try {
				odeConfigURL = odeConfigFile.toAbsolutePath().toUri().toURL();
			} catch (Exception e) {

			}
		}
		if (odeConfigURL == null) {
			odeConfigURL = Thread.currentThread().getContextClassLoader().getResource(odeConfig);
		}
		Map<String, Object> yamlConfig = null;
		if (odeConfigURL != null) {
			LoadSettings settings = new LoadSettingsBuilder().build();
			Load load = new Load(settings);
			try {
				for (Object o : load.loadAllFromInputStream(odeConfigURL.openStream())) {
					if (o instanceof Map) {
						yamlConfig = (Map<String, Object>) o;
						break;
					}
				}
				if (yamlConfig != null) {
					return new YAMLConfig(yamlConfig);
				}
			} catch (IOException e) {
				LOG.error("ODE YAML configuation error", e);
			}
		}
		return new YAMLConfig();
	}

	public static URL resolveODEUrl(String path) throws MalformedURLException {

		Path filePath = Paths.get(path);
		if (!filePath.isAbsolute()) {
			return getODEHome().resolve(filePath).toUri().toURL();
		}
		Path absoluteFilePath = filePath.toAbsolutePath();
		if (Files.exists(absoluteFilePath)) {
			return absoluteFilePath.toUri().toURL();
		}

		return Thread.currentThread().getContextClassLoader().getResource(path);
	}

	public static void configureIgnite(Config odeConfig, IgniteConfiguration igniteConfig) {
		ServiceLoader<IgniteConfigurator> igniteConfiguratorsLoader = ServiceLoader.load(IgniteConfigurator.class);
		List<IgniteConfigurator> igniteConfigurators = igniteConfiguratorsLoader.stream().map(p -> p.get()).sorted((p1, p2) -> {
			int p1p = Optional.ofNullable(p1.getClass().getAnnotation(Priority.class)).map(p -> p.value()).orElse(100);
			int p2p = Optional.ofNullable(p2.getClass().getAnnotation(Priority.class)).map(p -> p.value()).orElse(100);
			if (p1p == p2p) {
				return 0;
			}
			return p1p > p2p ? 1 : -1;

		}).collect(Collectors.toList());

		igniteConfigurators.forEach(c -> c.configure(odeConfig, igniteConfig));
	}

	@Override
	public void configure(Config config, IgniteConfiguration igniteConfig) {
		System.setProperty(IgniteSystemProperties.IGNITE_QUIET, "true");
		System.setProperty(IgniteSystemProperties.IGNITE_NO_ASCII, "true");
		System.setProperty(IgniteSystemProperties.IGNITE_PERFORMANCE_SUGGESTIONS_DISABLED, "true");

		Path odeHome = getODEHome();
		String odeTenant = getODETenant();

		Optional<Config> odeConfig = config.getConfig("ode");
		Optional<Config> igniteFileConfig = odeConfig.flatMap(c -> c.getConfig("ignite"));

		Optional<String> fileOdeHome = odeConfig.flatMap(c -> c.getString("home"));
		if (fileOdeHome.isPresent()) {
			odeHome = Paths.get(fileOdeHome.get()).toAbsolutePath();
		}

		Optional<String> fileOdeTenant = odeConfig.flatMap(c -> c.getString("tenant"));
		if (fileOdeTenant.isPresent()) {
			odeTenant = fileOdeTenant.get();
		}

		System.out.format("ODE HOME: %s\n", odeHome);
		System.out.format("ODE Tenant: %s\n", odeTenant);
		Path odeBaseDir = odeHome.resolve(odeTenant);
		// URL xml = Configuration.resolveODEUrl("ode-log4j2.xml");
		// IgniteLogger log = new Log4J2Logger(xml);

		Map<String, Object> attrs = new HashMap<>();
		attrs.put(ODE_HOME, odeHome.toString());
		attrs.put(ODE_TENANT, odeTenant);
		attrs.put(ODE_BASE_DIR, odeBaseDir.toString());

		// Discovery
		TcpCommunicationSpi commSpi = new TcpCommunicationSpi();
		igniteConfig.setCommunicationSpi(commSpi);
		TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
		igniteConfig.setDiscoverySpi(discoverySpi);
		TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
		discoverySpi.setIpFinder(ipFinder);

		igniteFileConfig.flatMap(c -> c.getConfig("discovery")).ifPresent(d -> {
			d.string("localAddress", v -> discoverySpi.setLocalAddress(v));
			d.number("localPort", v -> discoverySpi.setLocalPort(v));
			d.number("localPortRange", v -> discoverySpi.setLocalPortRange(v));
			d.list("finderAddresses", String.class, v -> ipFinder.setAddresses(v));
		});
		igniteFileConfig.flatMap(c -> c.getConfig("com")).ifPresent(c -> {
			c.number("localPort", v -> commSpi.setLocalPort(v));
			c.number("localPortRange", v -> commSpi.setLocalPortRange(v));
		});

		// Persistence
		DataStorageConfiguration storageCfg = new DataStorageConfiguration();
		storageCfg.getDefaultDataRegionConfiguration().setPersistenceEnabled(!igniteConfig.isClientMode());
		storageCfg.setStoragePath(odeBaseDir.resolve("db").toString());
		storageCfg.setWalPath(odeBaseDir.resolve(DataStorageConfiguration.DFLT_WAL_PATH).toString());
		storageCfg.setWalArchivePath(odeBaseDir.resolve(DataStorageConfiguration.DFLT_WAL_ARCHIVE_PATH).toString());

		igniteConfig.setDataStorageConfiguration(storageCfg);

		// Applying settings.
		// igniteConfig.setPluginConfigurations(pluginCfgs);
		igniteConfig.setPeerClassLoadingEnabled(false);
		igniteConfig.setActiveOnStart(true).setAutoActivationEnabled(true)
				// .setGridLogger(log)
				.setUserAttributes(attrs).setWorkDirectory(odeBaseDir.resolve("work").toAbsolutePath().toString());
		if (igniteConfig.isClientMode()) {
			igniteConfig.setIgniteInstanceName("ode-client-" + odeTenant);
		} else {
			igniteConfig.setIgniteInstanceName("ode-server-" + odeTenant);
		}

	}
}
