package org.apache.ode.test.core;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import javax.enterprise.inject.Any;

import org.apache.ignite.IgniteCluster;
import org.apache.ode.runtime.Server;
import org.apache.ode.spi.tenant.ClusterManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.io.TempDir;

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
		server2 = Server.instance();
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
		assertNotNull(server1);
		assertNotNull(server2);
		// try {
		// Path odeHome1 = testPath.resolve("ode-tenant-test1");
		Path odeHome1 = Paths.get("target/ode-tenant-test1");
		deleteDirectory(odeHome1);
		// Path odeHome2 = testPath.resolve("ode-tenant-test2");
		server1.start("ode-tenant-test.yml", odeHome1);
		IgniteCluster cluster = server1.ignite().cluster();
		ClusterManager manager = server1.container().select(ClusterManager.class, Any.Literal.INSTANCE).get();
		manager.activate(true);
		assertTrue(cluster.active());
		assertEquals(1, cluster.nodes().size());
		manager.baselineTopology(1l);
		assertEquals(1, cluster.currentBaselineTopology().size());
		server1.awaitInitalization(60, TimeUnit.SECONDS);

//		} catch (Exception e) {
//			e.printStackTrace();
//		}

//		assertNotNull(server.ignite());
//		Tenant tenant = server.ignite().services().serviceProxy(Tenant.SERVICE_NAME, Tenant.class, false);
//		assertNotNull(tenant);
//		assertEquals(TenantStatus.ONLINE, tenant.status());

	}
}
