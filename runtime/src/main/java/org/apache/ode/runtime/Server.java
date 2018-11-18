package org.apache.ode.runtime;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteException;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.lifecycle.LifecycleBean;
import org.apache.ignite.lifecycle.LifecycleEventType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.ode.runtime.owb.ODEOWBInitializer;
import org.apache.ode.runtime.owb.ODEOWBInitializer.ContainerMode;
import org.apache.ode.spi.config.Config;

public class Server implements LifecycleBean, AutoCloseable {

	public static final Logger LOG = LogManager.getLogger(Server.class);

	private static final Server SERVER = new Server();
	private SeContainer serverContainer;
	private Ignite ignite;

	private Server() {
	}

	public Server start(String configFile) {

		Config odeConfig = Configurator.loadConfigFile(configFile);

		IgniteConfiguration serverConfig = new IgniteConfiguration();
		serverConfig.setClientMode(false);
		serverConfig.setLifecycleBeans(this);
		Configurator.configureIgnite(odeConfig, serverConfig);

		ignite = Ignition.start(serverConfig);

		SeContainerInitializer initializer = SeContainerInitializer.newInstance().addProperty(ODEOWBInitializer.CONTAINER_MODE, ContainerMode.SERVER);
		serverContainer = initializer.initialize();

		// serverContainer.getBeanManager().fireEvent(new IgniteConfigurationEvent(serverConfig));
		// Configuration.initialize(serverConfig, configFile, false);
		// String odeWorkingDirectory = (String)
		// serverConfig.getUserAttributes().get(Configuration.ODE_WORK_DIR);
		// https://apacheignite.readme.io/docs/distributed-persistent-store

//							.setDiscoverySpi(new TcpDiscoverySpi()
		//
//									.setIpFinder(
		//
//											new TcpDiscoveryVmIpFinder()
		//
//													.setAddresses(Collections.singleton("127.0.0.1:47500..47502"))
		//
//									));

		// Ignite ignite = Ignition.start(serverConfig);

		// IgniteCluster cluster = ignite.cluster();
		// ClusterGroup tenantGroup = cluster.forAttribute(Configuration.ODE_TENANT, serverConfig.getUserAttributes().get(Configuration.ODE_TENANT));
		// tenantGroup.
		// Isolated Ignite Clusters https://apacheignite.readme.io/docs/tcpip-discovery
		// https://apacheignite.readme.io/docs/baseline-topology
		// IgniteCluster.setBaseLineTopogy()
		// try (Ignite server = Ignition.start(serverConfig);) {
		// Ignition.stop(false);
		// }

		// WebBeansContext context = ODEWebBeansContextProvider.getInstance().createRuntimeOWBContext(false);
		return this;
	}

	public static Server instance() {
		return SERVER;

	}

	public Ignite ignite() {
		if (ignite == null) {
			throw new IllegalStateException("Ignite unavailable");
		}
		return ignite;
	}

	public SeContainer container() {
		if (serverContainer == null) {
			throw new IllegalStateException("SeContainer unavailable");
		}
		return serverContainer;
	}

	@Override
	public void close() throws Exception {
		if (ignite != null) {
			ignite.close();
		}
		if (serverContainer != null) {
			serverContainer.close();
		}
		ignite = null;
		serverContainer = null;

	}

	@Override
	public void onLifecycleEvent(LifecycleEventType evt) throws IgniteException {
		if (evt == LifecycleEventType.BEFORE_NODE_START) {
			// start event
		} else if (evt == LifecycleEventType.AFTER_NODE_STOP) {
			// stop event

		}

	}

}
