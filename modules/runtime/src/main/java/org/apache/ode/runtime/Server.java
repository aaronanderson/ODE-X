package org.apache.ode.runtime;

import static org.apache.ode.spi.config.Config.ODE_ENVIRONMENT;
import static org.apache.ode.spi.config.Config.ODE_HOME;
import static org.apache.ode.spi.config.Config.ODE_TENANT;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCluster;
import org.apache.ignite.IgniteCompute;
import org.apache.ignite.IgniteException;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.lifecycle.LifecycleBean;
import org.apache.ignite.lifecycle.LifecycleEventType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.ode.runtime.cli.CLITask;
import org.apache.ode.runtime.deployment.AssemblyManagerImpl.AssembleContext;
import org.apache.ode.runtime.owb.ODEOWBInitializer;
import org.apache.ode.runtime.owb.ODEOWBInitializer.ContainerMode;
import org.apache.ode.spi.config.Config;
import org.apache.ode.spi.config.IgniteConfigureEvent;
import org.apache.ode.spi.deployment.Assembler.AssembleScoped;
import org.apache.ode.spi.tenant.Module;
import org.apache.ode.spi.tenant.Module.Id;
import org.apache.ode.spi.tenant.Tenant;
import org.apache.webbeans.annotation.DefaultLiteral;

/*
 Main ODE server process
 */
public class Server implements LifecycleBean, AutoCloseable, Extension {

	public static final Logger LOG = LogManager.getLogger(Server.class);

	private SeContainerInitializer serverContainerInitializer;
	private SeContainer serverContainer;
	private ServerClassLoader scl;

	// @IgniteInstanceResource
	private Ignite ignite;

	private Config odeConfig;

	private Server() {
		scl = new ServerClassLoader(Thread.currentThread().getContextClassLoader());
		ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(scl);
		try {
			serverContainerInitializer = SeContainerInitializer.newInstance().addProperty(ODEOWBInitializer.CONTAINER_MODE, ContainerMode.SERVER).addExtensions(this);
		} finally {
			Thread.currentThread().setContextClassLoader(oldCl);
		}
	}

	public Server start(String configFile) {
		return start(configFile, null, null);
	}

	/* Start the ODE service */
	public Server start(String configFile, String instanceName, Path odeHome) {
		// wrap in a custom classloader so that the OWB context is unique per server instance.
		ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(scl);
		try {
			odeConfig = Configurator.loadConfigFile(configFile);

			// CDI initialization should only do local classpath scanning and setup. Seperate ODE init/destroy events will be emitted to trigger interaction with Ignite for additional setup or teardown
			serverContainer = serverContainerInitializer.initialize();

			IgniteConfiguration serverConfig = new IgniteConfiguration();
			serverConfig.setClientMode(false);
			serverConfig.setClassLoader(scl);
			serverConfig.setUserAttributes(new HashMap<>());
			if (odeHome != null) {
				((Map<String, String>) serverConfig.getUserAttributes()).put(ODE_HOME, odeHome.toString());
			}
			if (instanceName != null) {
				serverConfig.setIgniteInstanceName(instanceName);
			}

			serverConfig.setLifecycleBeans(this);
			serverContainer.getBeanManager().fireEvent(new IgniteConfigureEvent(odeConfig, serverConfig));

			ignite = Ignition.start(serverConfig);

			if (odeConfig.getBool("ode.ignite.auto-cluster-activate").orElse(false)) {
				LOG.warn("Auto activation and initial baseline setting should only be enabled on a cluster with a single node");
				clusterAutoOnline();
			}

			IgniteCompute igniteCompute = ignite.compute();
			igniteCompute.localDeployTask(CLITask.class, scl);

//			TODO clean out test code below.
//			IgniteMessaging msg = ignite.message();
//			msg.remoteListen("MyOrderedTopic", (nodeId, imsg) -> {
//				System.out.println("Received ordered message [msg=" + imsg + ", from=" + nodeId + ']');
//				return true; // Return true to continue listening.
//			});
//			msg.sendOrdered("OrderedTopic", Integer.toString(1), 0);
//
//			igniteCompute.broadcast(new CDICallable<String>() {
//
//				@IgniteInstanceResource
//				private Ignite ignite;
//
//				@Inject
//				private Config config;
//
//				@Override
//				public String callExt() throws Exception {
//					String result = String.format("Cool %s %s", ignite, config);
//					System.out.println(result);
//					return result;
//					
//				}
//			});

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
		} finally {
			Thread.currentThread().setContextClassLoader(oldCl);
		}
	}

	public Server awaitInitalization(long timeout, TimeUnit unit) {
		if (ignite == null) {
			throw new IllegalStateException("Ignite unavailable");
		}
		if (!ignite.cluster().active()) {
			throw new IllegalStateException("Ignite cluster not activated");
		}
		// wait for service to become available. Service race conditions might be fixed in Ignite 2.8
		Tenant tenant = ignite.services().serviceProxy(Tenant.SERVICE_NAME, Tenant.class, false);

		long serviceStartTime = System.currentTimeMillis();
		boolean tenantReady = false;
		while (!tenantReady && System.currentTimeMillis() <= serviceStartTime + 60000) {
			try {
				tenant.awaitInitialization(timeout, unit);
				tenantReady = true;
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} catch (IgniteException ie) { // service unavailable
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
				}
			}
		}
		if (!tenantReady) {
			throw new IllegalStateException("Tenant service is unavailable");
		}

		// check consistency
		String localTenantName = (String) ignite.configuration().getUserAttributes().get(ODE_TENANT);
		String localTenantEnv = (String) ignite.configuration().getUserAttributes().get(ODE_ENVIRONMENT);
		if (!tenant.name().equals(localTenantName) || !tenant.environment().equals(localTenantEnv)) {
			throw new IllegalStateException(String.format("Tenant mismatch; remote: %s % local: %s %s ", tenant.name(), tenant.environment(), localTenantName, localTenantEnv));
		}
		Set<String> remoteModuleIds = tenant.modules();
		Set<String> localModuleIds = new HashSet<>();
		Instance<Module> localModules = serverContainer.select(Module.class, Any.Literal.INSTANCE);
		for (Module m : localModules) {
			Id id = m.getClass().getAnnotation(Id.class);
			if (id != null) {
				localModuleIds.add(id.value());
			} else {
				LOG.error(String.format("Module %s does not have required Id annotation", m.getClass()));
			}
		}
		if (!remoteModuleIds.equals(localModuleIds)) {
			throw new IllegalStateException(String.format("Tenant module id mismatch remote: %s local: %s", remoteModuleIds, localModuleIds));
		}

		return this;
	}

	public Server clusterAutoOnline() {
		IgniteCluster igniteCluster = ignite.cluster();
		if (!igniteCluster.active()) {
			igniteCluster.active(true);
			igniteCluster.setBaselineTopology(igniteCluster.topologyVersion());
			// this does the same thing
			// igniteCluster.setBaselineTopology(ignite.cluster().forServers().nodes());
		}
		awaitInitalization(60, TimeUnit.SECONDS);
		return this;
	}

	public static Server instance() {
		return new Server();
	}

	public Ignite ignite() {
		if (ignite == null) {
			throw new IllegalStateException("Ignite unavailable");
		}
		return ignite;
	}

	public SeContainerInitializer containerInitializer() {
		if (serverContainer != null) {
			throw new IllegalStateException("SeContainer already initialized");
		}
		return serverContainerInitializer;
	}

	public SeContainer container() {
		if (serverContainer == null) {
			throw new IllegalStateException("SeContainer unavailable");
		}
		return serverContainer;
	}

	public Config configuration() {
		if (odeConfig == null) {
			throw new IllegalStateException("Config unavailable");
		}
		return odeConfig;
	}

	@Override
	public void close() throws Exception {
		ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(scl);
		try {
			if (ignite != null) {
				ignite.close();
			}
			if (serverContainer != null) {
				serverContainer.close();
			}
			ignite = null;
			serverContainer = null;
			odeConfig = null;
			serverContainerInitializer = SeContainerInitializer.newInstance().addProperty(ODEOWBInitializer.CONTAINER_MODE, ContainerMode.SERVER).addExtensions(this);
		} finally {
			Thread.currentThread().setContextClassLoader(oldCl);
		}
	}

	@Override
	public void onLifecycleEvent(LifecycleEventType evt) throws IgniteException {
		if (evt == LifecycleEventType.AFTER_NODE_START) {

		} else if (evt == LifecycleEventType.AFTER_NODE_STOP) {
			// ODE stop event

		}

	}

	<T> void disableBeans(@Observes ProcessAnnotatedType<T> pat) {
		// System.out.format("Found type %s\n", pat.getAnnotatedType());

	}

	void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {
		bbd.addScope(AssembleScoped.class, false, false);
	}

	void addBeans(@Observes final AfterBeanDiscovery afb, final BeanManager bm) {
		// Ideally Ignite instance should be injected using IgniteResource or direct static lookup Ignition.localIgnite()
		afb.addBean().beanClass(Ignite.class).scope(ApplicationScoped.class).types(Ignite.class, Object.class).qualifiers(DefaultLiteral.INSTANCE).createWith(cc -> {
			return ignite();
		});

		afb.addBean().beanClass(Config.class).scope(ApplicationScoped.class).types(Config.class, Object.class).qualifiers(DefaultLiteral.INSTANCE).createWith(cc -> {
			return configuration();
		});

		afb.addContext(AssembleContext.instance());

//		afb.addBean().beanClass(Set.class).addType(new TypeLiteral<Map<String, Module>>() {
//		}).scope(ApplicationScoped.class).qualifiers(DefaultLiteral.INSTANCE).createWith(cc -> {
//			return loadModules(bm);
//		});

	}

	// gridClassLoader in IgniteUtils is set to this class's classloader.

	public static class ServerClassLoader extends ClassLoader {

		ServerClassLoader(ClassLoader parent) {
			super(parent);
		}

	}

}
