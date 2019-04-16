package org.apache.ode.runtime;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Priority;
import javax.enterprise.event.Observes;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteSystemProperties;
import org.apache.ignite.configuration.ConnectorConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.logger.log4j2.Log4J2Logger;
import org.apache.ignite.services.ServiceConfiguration;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.ode.runtime.tenant.TenantImpl;
import org.apache.ode.spi.config.Config;
import org.apache.ode.spi.config.IgniteConfigureEvent;
import org.apache.ode.spi.tenant.Tenant;
import org.snakeyaml.engine.v1.api.Load;
import org.snakeyaml.engine.v1.api.LoadSettings;
import org.snakeyaml.engine.v1.api.LoadSettingsBuilder;


public class Configurator {

	public static final String ODE_HOME = "ODE_HOME";
	public static final String ODE_BASE_DIR = "ODE_BASE_DIR";
	public static final String ODE_TENANT = "ODE_TENANT";
	public static final String ODE_CONFIG = "ODE_CONFIG";

	public static final String DEFAULT_HOST = "127.0.0.1";
	public static final int DEFAULT_PORT_RANGE = 20;
	public static final int DEFAULT_DISCOVERY_PORT = 48500;
	public static final int DEFAULT_COM_PORT = 48100;
	public static final int DEFAULT_CONNECTOR_PORT = 13211;

	public static final Logger LOG = LogManager.getLogger(Configurator.class);

	public void configure(@Observes @Priority(value = 1) IgniteConfigureEvent event) {
		Config config = event.config();
		IgniteConfiguration igniteConfig = event.igniteConfig();

		System.setProperty(IgniteSystemProperties.IGNITE_QUIET, "true");
		System.setProperty(IgniteSystemProperties.IGNITE_NO_ASCII, "true");
		System.setProperty(IgniteSystemProperties.IGNITE_PERFORMANCE_SUGGESTIONS_DISABLED, "true");
		// -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager should be set as a JVM option to ensure OWB logging is handled by log4j2

		Path odeHome = getODEHome();
		String odeTenant = getODETenant();

		Optional<Config> odeConfig = config.getConfig("ode");
		Optional<Config> igniteFileConfig = odeConfig.flatMap(c -> c.getConfig("ignite"));

		Optional<String> fileOdeHome = odeConfig.flatMap(c -> c.getString("home"));
		if (fileOdeHome.isPresent()) {
			odeHome = Paths.get(fileOdeHome.get()).toAbsolutePath();
		}
		if (igniteConfig.getUserAttributes().containsKey(ODE_HOME)) {
			odeHome = Paths.get((String) igniteConfig.getUserAttributes().get(ODE_HOME)).toAbsolutePath();
		}

		Optional<String> fileOdeTenant = odeConfig.flatMap(c -> c.getString("tenant"));
		if (fileOdeTenant.isPresent()) {
			odeTenant = fileOdeTenant.get();
		}

		LOG.info("ODE HOME: {}", odeHome);
		LOG.info("ODE Tenant: {}", odeTenant);
		Path odeBaseDir = odeHome.resolve(odeTenant);
		// URL xml = Configuration.resolveODEUrl("ode-log4j2.xml");
		// IgniteLogger log = new Log4J2Logger(xml);

		Map<String, Object> attrs = (Map<String, Object>) igniteConfig.getUserAttributes();
		attrs.put(ODE_HOME, odeHome.toString());
		attrs.put(ODE_TENANT, odeTenant);
		attrs.put(ODE_BASE_DIR, odeBaseDir.toString());

		// Persistence
		if (igniteFileConfig.map(c -> c.getBool("persistence-enabled")).orElse(Optional.of(false)).get() && !igniteConfig.isClientMode()) {
			DataStorageConfiguration storageCfg = new DataStorageConfiguration();
			storageCfg.getDefaultDataRegionConfiguration().setPersistenceEnabled(!igniteConfig.isClientMode());
			storageCfg.setStoragePath(odeBaseDir.resolve("db").toString());
			storageCfg.setWalPath(odeBaseDir.resolve(DataStorageConfiguration.DFLT_WAL_PATH).toString());
			storageCfg.setWalArchivePath(odeBaseDir.resolve(DataStorageConfiguration.DFLT_WAL_ARCHIVE_PATH).toString());

			igniteConfig.setDataStorageConfiguration(storageCfg);
		}

		try {
			URL logConfig = Thread.currentThread().getContextClassLoader().getResource("log4j2.xml");
			if (logConfig != null) {
				igniteConfig.setGridLogger(new Log4J2Logger(logConfig));
			}
		} catch (IgniteCheckedException e) {
			LOG.error("Ignite logging error", e);
		}

		// Discovery
		TcpCommunicationSpi commSpi = new TcpCommunicationSpi();
		igniteConfig.setCommunicationSpi(commSpi);
		TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
		igniteConfig.setDiscoverySpi(discoverySpi);
		TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
		discoverySpi.setIpFinder(ipFinder);
		ConnectorConfiguration conConfig = new ConnectorConfiguration();
		igniteConfig.setConnectorConfiguration(conConfig);

		discoverySpi.setLocalAddress(DEFAULT_HOST);
		discoverySpi.setLocalPort(DEFAULT_DISCOVERY_PORT);
		discoverySpi.setLocalPortRange(DEFAULT_PORT_RANGE);
		ipFinder.setAddresses(Stream.of(DEFAULT_HOST + ":" + DEFAULT_DISCOVERY_PORT + ".." + (DEFAULT_DISCOVERY_PORT + DEFAULT_PORT_RANGE)).collect(Collectors.toList()));
		commSpi.setLocalPort(DEFAULT_COM_PORT);
		commSpi.setLocalPortRange(DEFAULT_PORT_RANGE);
		conConfig.setHost(DEFAULT_HOST);
		conConfig.setPort(DEFAULT_CONNECTOR_PORT);
		conConfig.setPortRange(DEFAULT_PORT_RANGE);

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
		igniteFileConfig.flatMap(c -> c.getConfig("connector")).ifPresent(c -> {
			c.string("host", v -> conConfig.setHost(v));
			c.number("port", v -> conConfig.setPort(v));
			c.number("portRange", v -> conConfig.setPortRange(v));
		});

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

		// Initial services
		// Deploy tenant service singleton
		ServiceConfiguration tenant = new ServiceConfiguration().setMaxPerNodeCount(1).setTotalCount(1).setName(Tenant.SERVICE_NAME).setService(new TenantImpl());
		// ServiceConfiguration assemblyManager = new ServiceConfiguration().setMaxPerNodeCount(1).setName(AssemblyManager.SERVICE_NAME).setService(new AssemblyManagerImpl());
		igniteConfig.setServiceConfiguration(tenant);

	}

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
}
