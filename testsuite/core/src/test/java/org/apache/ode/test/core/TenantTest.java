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
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCluster;
import org.apache.ignite.services.ServiceDescriptor;
import org.apache.ode.runtime.Server;
import org.apache.ode.spi.CDIService;
import org.apache.ode.spi.tenant.Module;
import org.apache.ode.spi.tenant.Module.Id;
import org.apache.ode.spi.tenant.Module.ModuleStatus;
import org.apache.ode.spi.tenant.Tenant;
import org.apache.ode.spi.tenant.Tenant.TenantStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_CLASS)
public class TenantTest {
	Server server1 = null;
	Server server2 = null;

	// Ignite doesn't like the wal directory being in /tmp, files can be cleared by OS while test is running
//	@TempDir
//	Path testPath;

	@BeforeAll
	public void init() throws Exception {
		server1 = Server.instance();
		server1.containerInitializer().addBeanClasses(TestModule.class);
		server2 = Server.instance();
		server2.containerInitializer().addBeanClasses(TestModule.class);
		//
	}

	@AfterAll
	public void destroy() throws Exception {
		server1.close();
		server2.close();
		// "ode-tenant-test.yml"
	}

	private void deleteDirectory(Path directory) throws IOException {
		if (Files.exists(directory)) {
			Files.walk(directory).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
		}

	}

	@Test
	public void validate() throws Exception {
		// try {
		// Path odeHome1 = testPath.resolve("ode-tenant-test1");
		Path odeHome1 = Paths.get("target/ode-tenant-test1");
		deleteDirectory(odeHome1);
		// Path odeHome2 = testPath.resolve("ode-tenant-test2");
		server1.start("ode-tenant-test.yml", odeHome1);
		IgniteCluster cluster = server1.ignite().cluster();
		server1.clusterActivate(true);
		assertTrue(cluster.active());
		assertEquals(1, cluster.nodes().size());
		server1.clusterBaselineTopology(1l);
		assertEquals(1, cluster.currentBaselineTopology().size());
		server1.awaitInitalization(60, TimeUnit.SECONDS);
		Tenant tenant = server1.ignite().services().serviceProxy(Tenant.SERVICE_NAME, Tenant.class, false);
		assertNotNull(tenant);
		assertEquals(TenantStatus.OFFLINE, tenant.status());
		tenant.status(TenantStatus.ONLINE);
		assertEquals(TenantStatus.ONLINE, tenant.status());
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
		tenant.disable(Tenant.ALL_MODULES);
		assertEquals(ModuleStatus.DISABLED, tenant.status("TestModule"));
		assertTrue(testModule.isDisabled());
		for (ServiceDescriptor desc : server1.ignite().services().serviceDescriptors()) {
			if (TestService.SERVICE_NAME.equals(desc.name())) {
				fail(String.format("Test Service %s should have been cancelled by TestModule", TestService.SERVICE_NAME));
			}
		}

		// System.out.format("*********** Module List: %s ***********************\n", tenant.modules());

//		} catch (Exception e) {
//			e.printStackTrace();
//		}

//		assertNotNull(server.ignite());
//		Tenant tenant = server.ignite().services().serviceProxy(Tenant.SERVICE_NAME, Tenant.class, false);
//		assertNotNull(tenant);
//		assertEquals(TenantStatus.ONLINE, tenant.status());

	}

	@ApplicationScoped
	@Id("TestModule")
	public static class TestModule implements Module {

		private boolean enabled = false;
		private boolean disabled = false;

		@Enable
		public void enable(Ignite ignite) {
			ignite.services().deployNodeSingleton(TestService.SERVICE_NAME, new TestServiceImpl());
			enabled = true;
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
			return testModule.isEnabled();
		}

	}
}
