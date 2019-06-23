package org.apache.ode.test.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

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
import org.apache.ignite.igfs.IgfsPath;
import org.apache.ode.junit.OdeServer;
import org.apache.ode.runtime.Server;
import org.apache.ode.spi.config.MapConfig;
import org.apache.ode.spi.deployment.Assembler;
import org.apache.ode.spi.deployment.Assembler.AssembleScoped;
import org.apache.ode.spi.deployment.Assembler.AssemblyException;
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
	public static final String TEST_CONTENT_TYPE = "application/ode-test";
	Server server = null;
	Path resourceDirectory = null;

	public AssemblyTest(Server server) {
		this.server = server;
		this.resourceDirectory = Paths.get("src/test/resources/ode/assembly/test");
	}

	@Test
	public void lifecycle() throws Exception {
		AssemblyManager assemblyManager = server.ignite().services().service(AssemblyManager.SERVICE_NAME);
		assertNotNull(assemblyManager);
		TestAssembler testAssembly = server.container().select(TestAssembler.class, Any.Literal.INSTANCE).get();

//		Path targetDirectory = Paths.get("target");
//		Path testAssembly = targetDirectory.resolve("assembly-test.zip");
//		Files.deleteIfExists(testAssembly);
		// System.out.format("Environment: %s %s **********************\n", targetDirectory.toAbsolutePath(), resourceDirectory.toAbsolutePath());
//		try (FileSystem zipFS = FileSystems.newFileSystem(URI.create("jar:" + testAssembly.toUri()), Collections.singletonMap("create", "true"));) {
//			Files.copy(resourceDirectory.resolve("test.txt"), zipFS.getPath("test.txt"));
//		}

		URI resource = new URI("urn:org:apache:ode:assembly:test#lifecycle");

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

	@Test
	public void assembly() throws Exception {
		AssemblyManager assemblyManager = server.ignite().services().service(AssemblyManager.SERVICE_NAME);

		URI resource = new URI("urn:org:apache:ode:assembly:test#assemble");

		MapConfig config = new MapConfig();
		config.set("ode.test", true);

		assemblyManager.create(AssemblyDeploymentBuilder.instance().type(new URI(TEST_ASSEMBLY)).config(config).reference(resource).file(new FileEntry(resourceDirectory.resolve("assembler.test"))).build());
		assemblyManager.assemble(resource);

		TestAssembler testAssembly = server.container().select(TestAssembler.class, Any.Literal.INSTANCE).get();
		assertEquals(Boolean.TRUE, testAssembly.state().get("inspect"));
		assertEquals(Boolean.TRUE, testAssembly.state().get("validate"));
		assertEquals(Boolean.TRUE, testAssembly.state().get("compile"));

	}

	@ApplicationScoped
	@Assembler.AssemblyType(TEST_ASSEMBLY)
	public static class TestAssembler implements Assembler {

		private Map<String, Boolean> access = new HashMap<>();

		public Map<String, Boolean> state() {
			return access;
		}

		@Create
		public void create(Ignite ignite, BinaryObject assemblyTypeConfig, BinaryObjectBuilder assemblyBuilder, URI deploymentReference, IgfsPath path, MapConfig config) {
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

		@Stage("init")
		public void init(IgfsPath assemblyDir, TestAssembleContext context) {
			context.setSourcePath(assemblyDir);
			access.put("init", true);
		}

		@Stage("inspect")
		public void inspect(TestAssembleContext context) {
			access.put("inspect", context.getSourcePath() != null);
		}

		@Stage("validate")
		public void validate() {
			access.put("validate", true);
		}

		@Stage("compile")
		public void compile() {
			access.put("compile", true);
		}

		@Stage("complete")
		public void complete() {
			access.put("complete", true);
		}

	}

	@ApplicationScoped
	@Module.Id(TEST_ASSEMBLY)
	public static class TestModule implements Module {

		@Enable
		public void enable(Ignite ignite, UUID configKey) {
			IgniteCache<BinaryObject, BinaryObject> configCache = ignite.cache(Tenant.TENANT_CACHE_NAME).withKeepBinary();

			BinaryObjectBuilder contentType = ignite.binary().builder("Configuration");
			contentType.setField("oid", UUID.randomUUID());
			contentType.setField("type", "ode:contentType");
			contentType.setField("fileExtensions", new String[] { ".test" });
			contentType.setField("modifiedTime", ZonedDateTime.now(ZoneId.systemDefault()));
			configCache.put(Assembler.contentTypeConfigPath(ignite, TEST_CONTENT_TYPE), contentType.build());

			BinaryObjectBuilder assembly = ignite.binary().builder("Configuration");
			assembly.setField("oid", UUID.randomUUID());
			assembly.setField("type", "ode:assembly");
			assembly.setField("mode", "auto");
			assembly.setField("stages", new String[] { "init", "inspect", "validate", "compile", "complete" });
			assembly.setField("modifiedTime", ZonedDateTime.now(ZoneId.systemDefault()));
			configCache.put(Assembler.assemblyTypeConfigPath(ignite, TEST_ASSEMBLY), assembly.build());

		}

		@Disable
		public void disable(Ignite ignite) {
			IgniteCache<BinaryObject, BinaryObject> configCache = ignite.cache(Tenant.TENANT_CACHE_NAME).withKeepBinary();
			configCache.remove(Assembler.assemblyTypeConfigPath(ignite, TEST_ASSEMBLY));
			configCache.remove(Assembler.contentTypeConfigPath(ignite, TEST_CONTENT_TYPE));
		}

	}

	@AssembleScoped
	public static class TestAssembleContext {

		IgfsPath sourcePath;
		IgfsPath targetPath;

		public IgfsPath getSourcePath() {
			return sourcePath;
		}

		public void setSourcePath(IgfsPath sourcePath) {
			this.sourcePath = sourcePath;
		}

		public IgfsPath getTargetPath() {
			return targetPath;
		}

		public void setTargetPath(IgfsPath targetPath) {
			this.targetPath = targetPath;
		}

	}

}
