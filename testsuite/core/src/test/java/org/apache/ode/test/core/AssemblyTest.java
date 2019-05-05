package org.apache.ode.test.core;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.binary.BinaryObjectBuilder;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.cache.query.SqlQuery;
import org.apache.ode.junit.OdeServer;
import org.apache.ode.runtime.Server;
import org.apache.ode.spi.deployment.Assembly;
import org.apache.ode.spi.deployment.AssemblyManager;
import org.apache.ode.spi.deployment.AssemblyManager.Entry;
import org.apache.ode.spi.tenant.Module;
import org.apache.ode.spi.tenant.Tenant;
import org.junit.jupiter.api.Test;

@OdeServer
public class AssemblyTest {

	public static final String TEST_ASSEMBLY = "urn:org:apache:ode:test";
	Server server = null;

	public AssemblyTest(Server server) {
		this.server = server;
	}

	@Test
	public void lifecycle() throws Exception {
		AssemblyManager assemblyManager = server.ignite().services().service(AssemblyManager.SERVICE_NAME);
		assertNotNull(assemblyManager);

		Path resourceDirectory = Paths.get("src/test/resources/ode/assembly/test");

//		Path targetDirectory = Paths.get("target");
//		Path testAssembly = targetDirectory.resolve("assembly-test.zip");
//		Files.deleteIfExists(testAssembly);
		// System.out.format("Environment: %s %s **********************\n", targetDirectory.toAbsolutePath(), resourceDirectory.toAbsolutePath());
//		try (FileSystem zipFS = FileSystems.newFileSystem(URI.create("jar:" + testAssembly.toUri()), Collections.singletonMap("create", "true"));) {
//			Files.copy(resourceDirectory.resolve("test.txt"), zipFS.getPath("test.txt"));
//		}

		URI resource1 = new URI("urn:org:apache:ode:assembly:test#test1");
		URI resource2 = new URI("urn:org:apache:ode:assembly:test#test2");
//		assemblyManager.createAlias("test1", resource);
//		assertEquals(assemblyManager.alias("test1"), resource);
//		
//		
		assemblyManager.create(new URI(TEST_ASSEMBLY), resource1, null);
		assemblyManager.create(new URI(TEST_ASSEMBLY), resource2, null, new Entry("test/test.txt", resourceDirectory.resolve("test.txt")));
	}

	@Assembly.Id(TEST_ASSEMBLY)
	public static class TestAssembly implements Assembly {

	}

	@ApplicationScoped
	@Module.Id(TEST_ASSEMBLY)
	public static class TestModule implements Module {

		@Enable
		public void enable(Ignite ignite, UUID configKey) {
			IgniteCache<String, BinaryObject> configCache = ignite.cache(Tenant.TENANT_CACHE_NAME).withKeepBinary();
			BinaryObjectBuilder assemblies = ignite.binary().builder("Configuration");
			assemblies.setField("oid", UUID.randomUUID());
			assemblies.setField("type", "ode:assembly");
			assemblies.setField("modifiedTime", ZonedDateTime.now(ZoneId.systemDefault()));
			configCache.put("/ode:assemblies/" + TEST_ASSEMBLY, assemblies.build());
		}

		@Disable
		public void disable(Ignite ignite) {
			IgniteCache<String, BinaryObject> configCache = ignite.cache(Tenant.TENANT_CACHE_NAME).withKeepBinary();
			configCache.remove("/ode:assemblies/" + TEST_ASSEMBLY);
		}

	}

}
