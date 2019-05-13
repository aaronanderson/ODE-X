package org.apache.ode.test.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.binary.BinaryObjectBuilder;
import org.apache.ode.junit.OdeServer;
import org.apache.ode.runtime.Server;
import org.apache.ode.spi.config.MapConfig;
import org.apache.ode.spi.deployment.Assembly;
import org.apache.ode.spi.deployment.Assembly.AssemblyException;
import org.apache.ode.spi.deployment.AssemblyManager;
import org.apache.ode.spi.deployment.AssemblyManager.AssemblyDeployment;
import org.apache.ode.spi.deployment.AssemblyManager.AssemblyDeploymentBuilder;
import org.apache.ode.spi.deployment.Deployment.Entry;
import org.apache.ode.spi.deployment.Deployment.FileEntry;
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
		TestAssembly testAssembly = server.container().select(TestAssembly.class, Any.Literal.INSTANCE).get();

		Path resourceDirectory = Paths.get("src/test/resources/ode/assembly/test");

//		Path targetDirectory = Paths.get("target");
//		Path testAssembly = targetDirectory.resolve("assembly-test.zip");
//		Files.deleteIfExists(testAssembly);
		// System.out.format("Environment: %s %s **********************\n", targetDirectory.toAbsolutePath(), resourceDirectory.toAbsolutePath());
//		try (FileSystem zipFS = FileSystems.newFileSystem(URI.create("jar:" + testAssembly.toUri()), Collections.singletonMap("create", "true"));) {
//			Files.copy(resourceDirectory.resolve("test.txt"), zipFS.getPath("test.txt"));
//		}

		URI resource = new URI("urn:org:apache:ode:assembly:test#test");

		MapConfig config = new MapConfig();
		config.set("ode.test", true);

		assemblyManager.create(AssemblyDeploymentBuilder.instance().type(new URI(TEST_ASSEMBLY)).config(config).reference(resource).file(new FileEntry("test/test.txt", resourceDirectory.resolve("test.txt"))).build());
		assertEquals(Boolean.TRUE, testAssembly.state().get("create"));

		List<Entry> updatedFiles = new LinkedList<>();
		updatedFiles.add(new FileEntry("test/update.txt", resourceDirectory.resolve("update.txt")));
		assemblyManager.updateFiles(resource, updatedFiles);
		assertEquals(Boolean.TRUE, testAssembly.state().get("updateFiles"));

		config = new MapConfig();
		config.set("ode.update", true);
		assemblyManager.update(resource, config);
		assertEquals(Boolean.TRUE, testAssembly.state().get("updateConfig"));

		AssemblyDeployment<MapConfig> deployment = assemblyManager.export(resource);
		assertEquals(Boolean.TRUE, testAssembly.state().get("export"));
		assertNotNull(deployment.config());
		assertEquals(Boolean.TRUE, deployment.config().getBool("ode.test").get());
		assertEquals(Boolean.TRUE, deployment.config().getBool("ode.update").get());
		assertEquals(2, deployment.files().size());
		for (Entry file : deployment.files()) {
			if (file.path().contains("test.txt")) {
				assertEquals("test/test.txt", file.path());
				assertEquals("Test assembly file.", new Scanner(file.input().open()).useDelimiter("\\A").next());
			} else if (file.path().contains("update.txt")) {
				assertEquals("test/update.txt", file.path());
				assertEquals("Update assembly file.", new Scanner(file.input().open()).useDelimiter("\\A").next());
			} else {
				fail(String.format("Unexpected path %s", file.path()));
			}
		}

		assemblyManager.deploy(resource);
		assertEquals(Boolean.TRUE, testAssembly.state().get("deploy"));

		assemblyManager.undeploy(resource);
		assertEquals(Boolean.TRUE, testAssembly.state().get("undeploy"));

		assemblyManager.delete(resource);
		assertEquals(Boolean.TRUE, testAssembly.state().get("delete"));

	}

	@Test
	public void alias() throws Exception {
		AssemblyManager assemblyManager = server.ignite().services().service(AssemblyManager.SERVICE_NAME);

		URI resource = new URI("urn:org:apache:ode:assembly:test#test");
		assemblyManager.createAlias("test", resource);
		assertEquals(resource, assemblyManager.alias("test"));
		assemblyManager.deleteAlias("test");
		assertThrows(AssemblyException.class, () -> {
			assemblyManager.alias("test");
		});

	}

	@Assembly.Id(TEST_ASSEMBLY)
	@ApplicationScoped
	public static class TestAssembly implements Assembly {

		private Map<String, Boolean> access = new HashMap<>();

		public Map<String, Boolean> state() {
			return access;
		}

		@Create
		public void create(MapConfig config, BinaryObjectBuilder assemblyBuilder) {
			assemblyBuilder.setField("test", config.getBool("ode.test").get());
			access.put("create", true);
		}

		@Export
		public MapConfig export(BinaryObject assembly) {
			MapConfig config = new MapConfig();
			config.set("ode.test", assembly.field("test"));
			config.set("ode.update", assembly.field("update"));
			access.put("export", true);
			return config;
		}

		@Update
		public boolean updateConfig(MapConfig config, BinaryObjectBuilder assemblyBuilder) {
			assemblyBuilder.setField("update", config.getBool("ode.update").get());
			access.put("updateConfig", true);
			return true;
		}

		@Update(type = UpdateType.FILE)
		public boolean updateFiles() {
			access.put("updateFiles", true);
			return true;
		}

		@Deploy
		public void deploy() {
			access.put("deploy", true);
		}

		@Undeploy
		public void undeploy() {
			access.put("undeploy", true);
		}

		@Delete
		public void delete() {
			access.put("delete", true);
		}

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
