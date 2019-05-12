package org.apache.ode.runtime.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteFileSystem;
import org.apache.ignite.IgniteTransactions;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.binary.BinaryObjectBuilder;
import org.apache.ignite.binary.BinaryObjectException;
import org.apache.ignite.igfs.IgfsPath;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.transactions.Transaction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.ode.runtime.Util;
import org.apache.ode.runtime.Util.AnnotationFilter;
import org.apache.ode.runtime.Util.IgfsPathEntry;
import org.apache.ode.runtime.Util.InvocationFilter;
import org.apache.ode.runtime.Util.ParameterInitializer;
import org.apache.ode.spi.CDIService;
import org.apache.ode.spi.deployment.Assembly;
import org.apache.ode.spi.deployment.Assembly.AssemblyException;
import org.apache.ode.spi.deployment.Assembly.Create;
import org.apache.ode.spi.deployment.Assembly.Delete;
import org.apache.ode.spi.deployment.Assembly.Deploy;
import org.apache.ode.spi.deployment.Assembly.Export;
import org.apache.ode.spi.deployment.Assembly.Id;
import org.apache.ode.spi.deployment.Assembly.Undeploy;
import org.apache.ode.spi.deployment.Assembly.Update;
import org.apache.ode.spi.deployment.Assembly.UpdateType;
import org.apache.ode.spi.deployment.AssemblyManager;
import org.apache.ode.spi.deployment.Deployment.Entry;
import org.apache.ode.spi.tenant.Tenant;

public class AssemblyManagerImpl extends CDIService implements AssemblyManager {

	public static final Logger LOG = LogManager.getLogger(AssemblyManagerImpl.class);

	@IgniteInstanceResource
	private Ignite ignite;

	@Inject
	@Any
	private transient Instance<Assembly> assemblies;

	private transient Map<URI, Assembly> assemblyCache;

	@PostConstruct
	public void init() {
		assemblyCache = new HashMap<>();
		for (Assembly assembly : assemblies) {
			try {
				Id id = Optional.ofNullable(assembly.getClass().getAnnotation(Id.class)).orElseThrow(() -> new AssemblyException(String.format("Assembly %s does not have required Id annotation", assembly.getClass())));
				assemblyCache.put(new URI(id.value()), assembly);
			} catch (AssemblyException | URISyntaxException e) {
				LOG.error("assembly setup error", e);
			}

		}
	}

	@Override
	public <C> void create(AssemblyDeployment<C> deployment) throws AssemblyException {
		IgniteTransactions transactions = ignite.transactions();
		try (Transaction tx = transactions.txStart()) {
			Assembly assembly = Optional.ofNullable(assemblyCache.get(deployment.type())).orElseThrow(() -> new AssemblyException(String.format("Assembly type %s is unavailable", deployment.type())));
			IgniteCache<BinaryObject, BinaryObject> tenantCache = ignite.cache(Tenant.TENANT_CACHE_NAME).withKeepBinary();
			BinaryObject assemblyKey = ignite.binary().builder("AssemblyKey").setField("id", deployment.reference().toString()).build();

			UUID oid = UUID.randomUUID();
			IgniteFileSystem repositoryFS = ignite.fileSystem(Tenant.REPOSITORY_FILE_SYSTEM);
			IgfsPath assemblyPath = new IgfsPath(ASSEMBLY_DIR, oid.toString());
			if (!deployment.files().isEmpty()) {
				repositoryFS.mkdirs(assemblyPath);
				copyFiles(repositoryFS, assemblyPath, deployment.files());
			}
			BinaryObjectBuilder assemblyBuilder = ignite.binary().builder("Assembly");
			assemblyBuilder.setField("oid", oid);
			assemblyBuilder.setField("type", deployment.type().toString());
			assemblyBuilder.setField("createdTime", ZonedDateTime.now(ZoneId.systemDefault()));
			assemblyBuilder.setField("modifiedTime", ZonedDateTime.now(ZoneId.systemDefault()));
			assemblyBuilder.setField("deployed", false);

			try {
				Util.invoke(assembly, assembly.getClass(), new AnnotationFilter(Create.class), (args, types) -> {
					for (int i = 0; i < args.length; i++) {
						if (Ignite.class.equals(types[i])) {
							args[i] = ignite;
						} else if (BinaryObjectBuilder.class.equals(types[i])) {
							args[i] = assemblyBuilder;
						} else if (URI.class.equals(types[i])) {
							args[i] = deployment.reference();
						} else if (IgfsPath.class.equals(types[i])) {
							args[i] = assemblyPath;
						} else if (deployment.config().getClass().isAssignableFrom(types[i])) {
							args[i] = deployment.config();
						}
					}
				});
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new AssemblyException(e);
			}

			if (!tenantCache.putIfAbsent(assemblyKey, assemblyBuilder.build())) {
				if (repositoryFS.exists(assemblyPath)) {
					repositoryFS.delete(assemblyPath, true);
				}
				throw new AssemblyException(String.format("Assembly %s already exists", deployment.reference()));
			}

			tx.commit();
		}

		// igniteCache.

//		BinaryObjectBuilder moduleConfigBuilder = ignite.binary().builder(moduleConfig.getValue());
//		moduleConfigBuilder.setField("modifiedTime", ZonedDateTime.now(ZoneId.systemDefault()));
//		moduleConfigBuilder.setField("enabled", true);
//		IgniteCache<UUID, BinaryObject> configCache = ignite.cache(TENANT_CACHE_NAME).withKeepBinary();
//		configCache.put(moduleConfig.getKey(), moduleConfigBuilder.build());
	}

	private BinaryObject retrieveAssembly(URI reference) throws AssemblyException {
		IgniteCache<BinaryObject, BinaryObject> tenantCache = ignite.cache(Tenant.TENANT_CACHE_NAME).withKeepBinary();
		BinaryObject assemblyKey = ignite.binary().builder("AssemblyKey").setField("id", reference.toString()).build();
		BinaryObject assembly = tenantCache.get(assemblyKey);
		if (assembly == null) {
			throw new AssemblyException(String.format("Assembly %s is unavailable", reference));
		}
		return assembly;
	}

	private void copyFiles(IgniteFileSystem repositoryFS, IgfsPath assemblyPath, List<Entry> files) throws AssemblyException {
		for (Entry entry : files) {
			IgfsPath filePath = null;
			if (entry.path().contains("/")) {
				int endPathIndex = entry.path().lastIndexOf("/");
				IgfsPath parentPath = new IgfsPath(assemblyPath, entry.path().substring(0, endPathIndex));
				repositoryFS.mkdirs(parentPath);
				filePath = new IgfsPath(parentPath, entry.path().substring(endPathIndex + 1));
			} else {
				filePath = assemblyPath.suffix(entry.path());
			}

			try (InputStream is = entry.input().open(); OutputStream os = repositoryFS.create(filePath, false);) {
				is.transferTo(os);
			} catch (IOException e) {
				throw new AssemblyException(e);
			}
		}
	}

	@Override
	public <C> AssemblyDeployment<C> export(URI reference) throws AssemblyException {
		try {
			BinaryObject assembly = retrieveAssembly(reference);
			URI type = new URI((String) assembly.field("type"));
			AssemblyDeploymentBuilder<C> builder = AssemblyDeploymentBuilder.instance();

			builder.reference(reference);
			builder.type(type);

			IgniteFileSystem repositoryFS = ignite.fileSystem(Tenant.REPOSITORY_FILE_SYSTEM);
			IgfsPath assemblyPath = new IgfsPath(ASSEMBLY_DIR, ((UUID) assembly.field("oid")).toString());
			List<IgfsPathEntry> files = new LinkedList<>();
			Util.listFiles(repositoryFS, assemblyPath, files, 2);
			for (IgfsPathEntry file : files) {
				builder.file(file);
			}

			Assembly assemblyHandler = Optional.ofNullable(assemblyCache.get(type)).orElseThrow(() -> new AssemblyException(String.format("Assembly type %s is unavailable", type)));
			C config = Util.invoke(assemblyHandler, assemblyHandler.getClass(), new AnnotationFilter(Export.class), (args, types) -> {
				for (int i = 0; i < args.length; i++) {
					if (Ignite.class.equals(types[i])) {
						args[i] = ignite;
					} else if (URI.class.equals(types[i])) {
						args[i] = reference;
					} else if (IgfsPath.class.equals(types[i])) {
						args[i] = assemblyPath;
					} else if (BinaryObject.class.equals(types[i])) {
						args[i] = assembly;
					}
				}
			});
			builder.config(config);

			return builder.build();
		} catch (BinaryObjectException | URISyntaxException | IOException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new AssemblyException(e);
		}
	}

	private void assemblyUpdate(URI reference, UpdateType updateType, ParameterInitializer secondaryInitializer, List<Entry> files) throws AssemblyException {
		IgniteTransactions transactions = ignite.transactions();
		try (Transaction tx = transactions.txStart()) {
			BinaryObject assembly = retrieveAssembly(reference);
			URI type = new URI((String) assembly.field("type"));
			IgfsPath assemblyPath = new IgfsPath(ASSEMBLY_DIR, ((UUID) assembly.field("oid")).toString());
			Assembly assemblyHandler = Optional.ofNullable(assemblyCache.get(type)).orElseThrow(() -> new AssemblyException(String.format("Assembly type %s is unavailable", type)));
			BinaryObjectBuilder assemblyBuilder = ignite.binary().builder(assembly);

			if (updateType == UpdateType.FILE) {
				IgniteFileSystem repositoryFS = ignite.fileSystem(Tenant.REPOSITORY_FILE_SYSTEM);
				copyFiles(repositoryFS, assemblyPath, files);
			}

			Boolean assemblyUpdate = Util.invoke(assemblyHandler, assemblyHandler.getClass(), (m) -> {
				Update update = m.getDeclaredAnnotation(Update.class);
				return update != null && update.type() == updateType;
			}, (args, types) -> {
				for (int i = 0; i < args.length; i++) {
					if (Ignite.class.equals(types[i])) {
						args[i] = ignite;
					} else if (URI.class.equals(types[i])) {
						args[i] = reference;
					} else if (IgfsPath.class.equals(types[i])) {
						args[i] = assemblyPath;
					} else if (BinaryObjectBuilder.class.equals(types[i])) {
						args[i] = assemblyBuilder;
					}
				}
				secondaryInitializer.initialize(args, types);
			});

			if (assemblyUpdate != null && assemblyUpdate) {
				IgniteCache<BinaryObject, BinaryObject> tenantCache = ignite.cache(Tenant.TENANT_CACHE_NAME).withKeepBinary();
				BinaryObject assemblyKey = ignite.binary().builder("AssemblyKey").setField("id", reference.toString()).build();
				tenantCache.put(assemblyKey, assemblyBuilder.build());
				tx.commit();
			}
		} catch (BinaryObjectException | URISyntaxException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new AssemblyException(e);
		}
	}

	@Override
	public <C> void update(URI reference, C config) throws AssemblyException {
		assemblyUpdate(reference, UpdateType.CONFIG, (args, types) -> {
			for (int i = 0; i < args.length; i++) {
				if (args[i] == null && config.getClass().isAssignableFrom(types[i])) {
					args[i] = config;
				}
			}
		}, Collections.EMPTY_LIST);
	}

	@Override
	public void updateFiles(URI reference, List<Entry> files) throws AssemblyException {
		assemblyUpdate(reference, UpdateType.FILE, (args, types) -> {
			for (int i = 0; i < args.length; i++) {
				if (args[i] == null && List.class.isAssignableFrom(types[i])) {
					List<String> filePaths = new LinkedList<>();
					for (Entry file : files) {
						filePaths.add(file.path());
					}
					args[i] = filePaths;
				}
			}
		}, files);
	}

	@Override
	public void delete(URI reference) throws AssemblyException {
		try {

			IgniteTransactions transactions = ignite.transactions();
			try (Transaction tx = transactions.txStart()) {
				BinaryObject assembly = retrieveAssembly(reference);
				URI type = new URI((String) assembly.field("type"));

				Assembly assemblyHandler = Optional.ofNullable(assemblyCache.get(type)).orElseThrow(() -> new AssemblyException(String.format("Assembly type %s is unavailable", type)));

				IgniteFileSystem repositoryFS = ignite.fileSystem(Tenant.REPOSITORY_FILE_SYSTEM);
				IgfsPath assemblyPath = new IgfsPath(ASSEMBLY_DIR, ((UUID) assembly.field("oid")).toString());

				Util.invoke(assemblyHandler, assemblyHandler.getClass(), new AnnotationFilter(Delete.class), (args, types) -> {
					for (int i = 0; i < args.length; i++) {
						if (Ignite.class.equals(types[i])) {
							args[i] = ignite;
						} else if (URI.class.equals(types[i])) {
							args[i] = reference;
						} else if (BinaryObject.class.equals(types[i])) {
							args[i] = assembly;
						} else if (IgfsPath.class.equals(types[i])) {
							args[i] = assemblyPath;
						}
					}
				});
				if (repositoryFS.exists(assemblyPath)) {
					repositoryFS.delete(assemblyPath, true);
				}
				IgniteCache<BinaryObject, BinaryObject> tenantCache = ignite.cache(Tenant.TENANT_CACHE_NAME).withKeepBinary();
				BinaryObject assemblyKey = ignite.binary().builder("AssemblyKey").setField("id", reference.toString()).build();
				tenantCache.remove(assemblyKey);
				tx.commit();
			}
		} catch (BinaryObjectException | URISyntaxException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new AssemblyException(e);
		}
	}

	private void deploymentUpdate(URI reference, boolean deploy) throws AssemblyException {
		try {
			IgniteTransactions transactions = ignite.transactions();
			try (Transaction tx = transactions.txStart()) {
				BinaryObject assembly = retrieveAssembly(reference);
				URI type = new URI((String) assembly.field("type"));

				Assembly assemblyHandler = Optional.ofNullable(assemblyCache.get(type)).orElseThrow(() -> new AssemblyException(String.format("Assembly type %s is unavailable", type)));
				BinaryObjectBuilder assemblyBuilder = ignite.binary().builder(assembly);
				assemblyBuilder.setField("deployed", deploy);

				Util.invoke(assemblyHandler, assemblyHandler.getClass(), new AnnotationFilter(deploy ? Deploy.class : Undeploy.class), (args, types) -> {
					for (int i = 0; i < args.length; i++) {
						if (Ignite.class.equals(types[i])) {
							args[i] = ignite;
						} else if (URI.class.equals(types[i])) {
							args[i] = reference;
						} else if (IgfsPath.class.equals(types[i])) {
							IgfsPath assemblyPath = new IgfsPath(ASSEMBLY_DIR, ((UUID) assembly.field("oid")).toString());
							args[i] = assemblyPath;
						} else if (BinaryObjectBuilder.class.equals(types[i])) {
							args[i] = assemblyBuilder;
						}
					}
				});

				IgniteCache<BinaryObject, BinaryObject> tenantCache = ignite.cache(Tenant.TENANT_CACHE_NAME).withKeepBinary();
				BinaryObject assemblyKey = ignite.binary().builder("AssemblyKey").setField("id", reference.toString()).build();
				tenantCache.put(assemblyKey, assemblyBuilder.build());
				tx.commit();

			}
		} catch (BinaryObjectException | URISyntaxException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new AssemblyException(e);
		}

	}

	@Override
	public void deploy(URI reference) throws AssemblyException {
		deploymentUpdate(reference, true);
	}

	@Override
	public void undeploy(URI reference) throws AssemblyException {
		deploymentUpdate(reference, false);

	}

	@Override
	public void createAlias(String alias, URI reference) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteAlias(String alias) {
		// TODO Auto-generated method stub

	}

	@Override
	public URI alias(String alias) {
		// TODO Auto-generated method stub
		return null;
	}

}
