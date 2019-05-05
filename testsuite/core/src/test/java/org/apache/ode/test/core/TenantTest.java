package org.apache.ode.test.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.cache.Cache.Entry;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCluster;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.cache.query.SqlQuery;
import org.apache.ignite.services.ServiceDescriptor;
import org.apache.ode.runtime.Server;
import org.apache.ode.spi.CDIService;
import org.apache.ode.spi.tenant.Module;
import org.apache.ode.spi.tenant.Module.Id;
import org.apache.ode.spi.tenant.Module.ModuleStatus;
import org.apache.ode.spi.tenant.Tenant;
import org.apache.ode.spi.tenant.Tenant.TenantStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_CLASS)
public class TenantTest {

	// Ignite doesn't like the wal directory being in /tmp, files can be cleared by OS while test is running
//	@TempDir
//	Path testPath;

	private void deleteDirectory(Path directory) throws IOException {
		if (Files.exists(directory)) {
			Files.walk(directory).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
		}

	}

	@Test
	public void modules() throws Exception {
		try (Server server1 = Server.instance();) {
			server1.containerInitializer().addBeanClasses(TestModule.class);

			// Path odeHome1 = testPath.resolve("ode-tenant-test1");
			Path odeHome1 = Paths.get("target/tenant-test1");
			deleteDirectory(odeHome1);
			Path odeHome2 = Paths.get("target/tenant-test2");
			deleteDirectory(odeHome2);
			server1.start("ode-tenant-test.yml", "ode-server-tenant-test1", odeHome1);
			IgniteCluster cluster = server1.ignite().cluster();
			cluster.active(true);
			assertTrue(cluster.active());
			assertEquals(1, cluster.nodes().size());
			cluster.setBaselineTopology(cluster.topologyVersion());
			assertEquals(1, cluster.currentBaselineTopology().size());
			server1.awaitInitalization(60, TimeUnit.SECONDS);
			Tenant tenant = server1.ignite().services().serviceProxy(Tenant.SERVICE_NAME, Tenant.class, false);
			assertNotNull(tenant);
			TestModule testModule = server1.container().select(TestModule.class, Any.Literal.INSTANCE).get();
			assertTrue(tenant.modules().contains("TestModule"));
			assertEquals(ModuleStatus.DISABLED, tenant.status("TestModule"));
			assertFalse(testModule.isEnabled());
			tenant.enable(Tenant.ALL_MODULES);
			assertEquals(ModuleStatus.ENABLED, tenant.status("TestModule"));
			TestService testService = server1.ignite().services().serviceProxy(TestService.SERVICE_NAME, TestService.class, false);
			assertNotNull(testService);
			assertTrue(testService.online());
			assertTrue(testModule.isEnabled());
			assertFalse(testModule.isDisabled());

			assertEquals(TenantStatus.OFFLINE, tenant.status());
			tenant.status(TenantStatus.ONLINE);
			assertEquals(TenantStatus.ONLINE, tenant.status());

			tenant.disable(Tenant.ALL_MODULES);
			assertEquals(ModuleStatus.DISABLED, tenant.status("TestModule"));
			assertTrue(testModule.isDisabled());
			for (ServiceDescriptor desc : server1.ignite().services().serviceDescriptors()) {
				if (TestService.SERVICE_NAME.equals(desc.name())) {
					fail(String.format("Test Service %s should have been cancelled by TestModule", TestService.SERVICE_NAME));
				}
			}

		}

	}

	@Test
	public void failover() throws Exception {
		try (Server server1 = Server.instance(); Server server2 = Server.instance();) {
			server1.containerInitializer().addBeanClasses(TestModule.class);
			server2.containerInitializer().addBeanClasses(TestModule.class);

			Path odeHome1 = Paths.get("target/tenant-test1");
			deleteDirectory(odeHome1);
			Path odeHome2 = Paths.get("target/tenant-test2");
			deleteDirectory(odeHome2);
			// start server one, set cluster of 1 node
			server1.start("ode-tenant-test.yml", "ode-server-tenant-test1", odeHome1);
			UUID server1Id = server1.ignite().cluster().localNode().id();
			IgniteCluster cluster = server1.ignite().cluster();
			assertEquals(1, cluster.topologyVersion());
			assertEquals(1, cluster.nodes().size());
			cluster.active(true);
			assertEquals(1, cluster.currentBaselineTopology().size());
			server1.awaitInitalization(60, TimeUnit.SECONDS);

			// affirm tenant service available, start all modules
			Tenant tenant = server1.ignite().services().serviceProxy(Tenant.SERVICE_NAME, Tenant.class, false);
			assertNotNull(tenant);
			assertEquals(TenantStatus.OFFLINE, tenant.status());
			tenant.enable(Tenant.ALL_MODULES);
			tenant.status(TenantStatus.ONLINE);

			// start second server, affirm cluster size updated accordingly
			server2.start("ode-tenant-test.yml", "ode-server-tenant-test2", odeHome2);
			UUID server2Id = server2.ignite().cluster().localNode().id();
			assertEquals(2, cluster.nodes().size());
			assertEquals(2, cluster.topologyVersion());
			assertEquals(1, cluster.currentBaselineTopology().size());
			cluster.setBaselineTopology(2l);
			assertEquals(2, cluster.currentBaselineTopology().size());
			server2.awaitInitalization(60, TimeUnit.SECONDS);
			for (ServiceDescriptor desc : server1.ignite().services().serviceDescriptors()) {
				if (Tenant.SERVICE_NAME.equals(desc.name())) {
					assertEquals(1, desc.topologySnapshot().get(server1Id));
					assertEquals(0, desc.topologySnapshot().get(server2Id));
				} else if (TestService.SERVICE_NAME.equals(desc.name())) {
					assertEquals(1, desc.topologySnapshot().get(server1Id));
					assertEquals(1, desc.topologySnapshot().get(server2Id));
				}
			}

			assertTrue(server1.ignite().cache(Tenant.TENANT_CACHE_NAME).withKeepBinary().containsKey(server1.ignite().binary().builder("ConfigurationKey").setField("path", "/ode:tenant").build()));
			assertTrue(server2.ignite().cache(Tenant.TENANT_CACHE_NAME).withKeepBinary().containsKey(server2.ignite().binary().builder("ConfigurationKey").setField("path", "/ode:tenant").build()));
			tenant = server2.ignite().services().serviceProxy(Tenant.SERVICE_NAME, Tenant.class, false);
			assertNotNull(tenant);
			assertEquals(TenantStatus.ONLINE, tenant.status());
			TestService testService = server2.ignite().services().serviceProxy(TestService.SERVICE_NAME, TestService.class, false);
			assertNotNull(testService);
			assertTrue(testService.online());
			
			Thread.sleep(100); //allow time for cache replication to complete.
			// test failover
			server1.close();
			assertTrue(server2.ignite().cache(Tenant.TENANT_CACHE_NAME).withKeepBinary().containsKey(server2.ignite().binary().builder("ConfigurationKey").setField("path", "/ode:tenant").build()));
			cluster = server2.ignite().cluster();
			assertEquals(1, cluster.nodes().size());
			assertEquals(2, cluster.currentBaselineTopology().size());
			assertEquals(TenantStatus.ONLINE, tenant.status());
			assertTrue(testService.online());

		}

	}

	@Test
	public void restart() throws Exception {
		try (Server server = Server.instance();) {
			server.containerInitializer().addBeanClasses(TestModule.class);

			Path odeHome = Paths.get("target/tenant-test");
			deleteDirectory(odeHome);

			server.start("ode-test.yml", "ode-server-tenant-test", odeHome);
			Tenant tenant = server.ignite().services().serviceProxy(Tenant.SERVICE_NAME, Tenant.class, false);
			assertNotNull(tenant);
			assertEquals(TenantStatus.ONLINE, tenant.status());
			TestService testService = server.ignite().services().serviceProxy(TestService.SERVICE_NAME, TestService.class, false);
			assertNotNull(testService);
			assertTrue(testService.online());

			server.close();

			server.containerInitializer().addBeanClasses(TestModule.class);
			server.start("ode-test.yml", "ode-server-tenant-test", odeHome);

			tenant = server.ignite().services().serviceProxy(Tenant.SERVICE_NAME, Tenant.class, false);
			assertNotNull(tenant);
			assertEquals(TenantStatus.ONLINE, tenant.status());
			testService = server.ignite().services().serviceProxy(TestService.SERVICE_NAME, TestService.class, false);
			assertNotNull(testService);
			assertTrue(testService.online());

		}

	}

	@ApplicationScoped
	@Id("TestModule")
	public static class TestModule implements Module {

		private boolean enabled = false;
		private boolean disabled = false;

		@Enable
		public void enable(Ignite ignite, UUID configKey) {
			ignite.services().deployNodeSingleton(TestService.SERVICE_NAME, new TestServiceImpl());

			List<Entry<String, BinaryObject>> moduleConfig = ignite.cache(Tenant.TENANT_CACHE_NAME).withKeepBinary().query(new SqlQuery<String, BinaryObject>("Configuration", "oid = ?").setArgs(configKey)).getAll();

			enabled = !moduleConfig.isEmpty();
		}

		@Disable
		public void disable(Ignite ignite) {
			ignite.services().cancel(TestService.SERVICE_NAME);
			disabled = true;
		}

		public boolean isEnabled() {
			return enabled;
		}

		public boolean isDisabled() {
			return disabled;
		}

	}

	public static interface TestService {
		public static final String SERVICE_NAME = "urn:org:apache:ode:test_service";

		public boolean online();
	}

	public static class TestServiceImpl extends CDIService implements TestService {

		@Inject
		@Id("TestModule")
		transient TestModule testModule;

		@Override
		public boolean online() {
			return testModule != null;
		}

	}
}
