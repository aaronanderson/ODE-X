package org.apache.ode.runtime.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.apache.ode.runtime.Util.DefaultParameterInitializer;
import org.apache.ode.runtime.Util.IgfsPathEntry;
import org.apache.ode.runtime.Util.Invocation;
import org.apache.ode.runtime.Util.MethodIndexer;
import org.apache.ode.runtime.Util.ParameterInitializer;
import org.apache.ode.spi.CDIService;
import org.apache.ode.spi.ContextImpl;
import org.apache.ode.spi.deployment.Assembler;
import org.apache.ode.spi.deployment.Assembler.AssembleScoped;
import org.apache.ode.spi.deployment.Assembler.AssemblyException;
import org.apache.ode.spi.deployment.Assembler.AssemblyType;
import org.apache.ode.spi.deployment.Assembler.Create;
import org.apache.ode.spi.deployment.Assembler.Delete;
import org.apache.ode.spi.deployment.Assembler.Deploy;
import org.apache.ode.spi.deployment.Assembler.Export;
import org.apache.ode.spi.deployment.Assembler.Priority;
import org.apache.ode.spi.deployment.Assembler.Stage;
import org.apache.ode.spi.deployment.Assembler.Undeploy;
import org.apache.ode.spi.deployment.Assembler.Update;
import org.apache.ode.spi.deployment.Assembler.UpdateType;
import org.apache.ode.spi.deployment.AssemblyManager;
import org.apache.ode.spi.deployment.Deployment.Entry;
import org.apache.ode.spi.tenant.Tenant;

public class AssemblyManagerImpl extends CDIService implements AssemblyManager {

	public static final Logger LOG = LogManager.getLogger(AssemblyManagerImpl.class);

	@IgniteInstanceResource
	private Ignite ignite;

	@Inject
	@Any
	private transient Instance<Assembler> assemblies;

	private transient Map<AssemblyOperation, List<Invocation<Assembler>>> assemblyCache;

	@PostConstruct
	public void init() {
		assemblyCache = new HashMap<>();
		try {
			Util.index(assemblies, assemblyCache, new AssemblyOperationIndexer());
//				Id id = Optional.ofNullable(assembly.getClass().getAnnotation(Id.class)).orElseThrow(() -> new AssemblyException(String.format("Assembly %s does not have required Id annotation", assembly.getClass())));
//				assemblyCache.put(new URI(id.value()), assembly);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			LOG.error("assembly setup error", e);
		}

	}

	@Override
	public <C> void create(AssemblyDeployment<C> deployment) throws AssemblyException {
		String assemblyType = deployment.type().toString();
		BinaryObject assemblyTypeConfig = assemblyTypeConfig(ignite, assemblyType);

		IgniteTransactions transactions = ignite.transactions();
		try (Transaction tx = transactions.txStart()) {

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
			assemblyBuilder.setField("dependencies", deployment.dependencies().stream().map(u -> u.toString()).toArray(String[]::new));
			ParameterInitializer initializer = new DefaultParameterInitializer(null, ignite, assemblyTypeConfig, assemblyBuilder, deployment.reference(), assemblyPath, deployment.config());
			Util.invoke(new AssemblyOperation(Create.class, assemblyType), assemblyCache, initializer, e -> new AssemblyException(e));

			if (!tenantCache.putIfAbsent(assemblyKey, assemblyBuilder.build())) {
				if (repositoryFS.exists(assemblyPath)) {
					repositoryFS.delete(assemblyPath, true);
				}
				throw new AssemblyException(String.format("Assembly %s already exists", deployment.reference()));
			}

			tx.commit();
		}

		assemble(deployment.reference(), false);
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
				filePath = new IgfsPath(assemblyPath, entry.path());
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
			URI assemblyType = new URI((String) assembly.field("type"));
			AssemblyDeploymentBuilder<C> builder = AssemblyDeploymentBuilder.instance();

			builder.reference(reference);
			builder.type(assemblyType);

			IgniteFileSystem repositoryFS = ignite.fileSystem(Tenant.REPOSITORY_FILE_SYSTEM);
			IgfsPath assemblyPath = new IgfsPath(ASSEMBLY_DIR, ((UUID) assembly.field("oid")).toString());
			List<IgfsPathEntry> files = new LinkedList<>();
			Util.listFiles(repositoryFS, assemblyPath, files, 2);
			for (IgfsPathEntry file : files) {
				builder.file(file);
			}

			ParameterInitializer initializer = new DefaultParameterInitializer(null, ignite, reference, assemblyPath, assembly);
			C config = Util.invoke(new AssemblyOperation(Export.class, assemblyType.toString()), assemblyCache, initializer, e -> new AssemblyException(e));
			builder.config(config);

			return builder.build();
		} catch (BinaryObjectException | URISyntaxException | IOException | IllegalArgumentException e) {
			throw new AssemblyException(e);
		}
	}

	private void assemblyUpdate(URI reference, UpdateType updateType, ParameterInitializer secondaryInitializer, List<Entry> files) throws AssemblyException {
		IgniteTransactions transactions = ignite.transactions();
		try (Transaction tx = transactions.txStart()) {
			BinaryObject assembly = retrieveAssembly(reference);
			URI assemblyType = new URI((String) assembly.field("type"));
			IgfsPath assemblyPath = new IgfsPath(ASSEMBLY_DIR, ((UUID) assembly.field("oid")).toString());
			BinaryObjectBuilder assemblyBuilder = ignite.binary().builder(assembly);

			if (updateType == UpdateType.FILE) {
				IgniteFileSystem repositoryFS = ignite.fileSystem(Tenant.REPOSITORY_FILE_SYSTEM);
				copyFiles(repositoryFS, assemblyPath, files);
			}

			ParameterInitializer initializer = new DefaultParameterInitializer(null, ignite, reference, assemblyPath, assemblyBuilder);
			Boolean assemblyUpdate = Util.invoke(new AssemblyOperation(assemblyType.toString(), updateType), assemblyCache, (args, types) -> {
				initializer.initialize(args, types);
				secondaryInitializer.initialize(args, types);
			}, e -> new AssemblyException(e));

			if (assemblyUpdate != null && assemblyUpdate) {
				IgniteCache<BinaryObject, BinaryObject> tenantCache = ignite.cache(Tenant.TENANT_CACHE_NAME).withKeepBinary();
				BinaryObject assemblyKey = ignite.binary().builder("AssemblyKey").setField("id", reference.toString()).build();
				tenantCache.put(assemblyKey, assemblyBuilder.build());
				tx.commit();
			}
		} catch (BinaryObjectException | URISyntaxException | IllegalArgumentException e) {
			throw new AssemblyException(e);
		}

		assemble(reference, false);
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
	public void assemble(URI reference) throws AssemblyException {
		assemble(reference, true);
	}

	private void assemble(URI reference, boolean manual) throws AssemblyException {
		//
		try {
			IgniteCache<BinaryObject, BinaryObject> tenantCache = ignite.cache(Tenant.TENANT_CACHE_NAME).withKeepBinary();
			BinaryObject assemblyKey = ignite.binary().builder("AssemblyKey").setField("id", reference.toString()).build();
			BinaryObject assembly = tenantCache.get(assemblyKey);
			if (assembly == null) {
				throw new AssemblyException(String.format("Assembly %s is unavailable", reference));
			}

			URI assemblyType = new URI((String) assembly.field("type"));
			IgfsPath assemblyPath = new IgfsPath(ASSEMBLY_DIR, ((UUID) assembly.field("oid")).toString());

			BinaryObject assemblyTypeConfig = assemblyTypeConfig(ignite, assemblyType.toString());
			String mode = assemblyTypeConfig.field("mode");
			String[] stages = assemblyTypeConfig.field("stages");

			if (stages != null) {
				AssembleContext assembleContext = AssembleContext.instance();
				try {
					assembleContext.start();
					for (String stage : stages) {

						IgniteTransactions transactions = ignite.transactions();
						try (Transaction tx = transactions.txStart();) {
							assembly = tenantCache.get(assemblyKey);
							BinaryObjectBuilder assemblyBuilder = ignite.binary().builder(assembly);

							ParameterInitializer initializer = new DefaultParameterInitializer(t -> assembleContext.instance(t, Any.Literal.INSTANCE), ignite, reference, assemblyPath, assemblyBuilder);
							Util.invoke(new AssemblyOperation(assemblyType.toString(), stage), assemblyCache, initializer, e -> new AssemblyException(e));
							tenantCache.put(assemblyKey, assemblyBuilder.build());
							tx.commit();
						}
					}
				} finally {
					assembleContext.end();
				}
			}

		} catch (BinaryObjectException | URISyntaxException | IllegalArgumentException e) {
			throw new AssemblyException(e);
		}
	}

	@Override
	public void delete(URI reference) throws AssemblyException {
		try {

			IgniteTransactions transactions = ignite.transactions();
			try (Transaction tx = transactions.txStart()) {
				BinaryObject assembly = retrieveAssembly(reference);
				URI assemblyType = new URI((String) assembly.field("type"));

				IgniteFileSystem repositoryFS = ignite.fileSystem(Tenant.REPOSITORY_FILE_SYSTEM);
				IgfsPath assemblyPath = new IgfsPath(ASSEMBLY_DIR, ((UUID) assembly.field("oid")).toString());

				ParameterInitializer initializer = new DefaultParameterInitializer(null, ignite, reference, assemblyPath, assembly);
				Util.invoke(new AssemblyOperation(Delete.class, assemblyType.toString()), assemblyCache, initializer, e -> new AssemblyException(e));
				if (repositoryFS.exists(assemblyPath)) {
					repositoryFS.delete(assemblyPath, true);
				}
				IgniteCache<BinaryObject, BinaryObject> tenantCache = ignite.cache(Tenant.TENANT_CACHE_NAME).withKeepBinary();
				BinaryObject assemblyKey = ignite.binary().builder("AssemblyKey").setField("id", reference.toString()).build();
				tenantCache.remove(assemblyKey);
				tx.commit();
			}
		} catch (BinaryObjectException | URISyntaxException | IllegalArgumentException e) {
			throw new AssemblyException(e);
		}
	}

	private void deploymentUpdate(URI reference, boolean deploy) throws AssemblyException {
		try {
			IgniteTransactions transactions = ignite.transactions();
			try (Transaction tx = transactions.txStart()) {
				BinaryObject assembly = retrieveAssembly(reference);
				URI assemblyType = new URI((String) assembly.field("type"));

				BinaryObjectBuilder assemblyBuilder = ignite.binary().builder(assembly);
				assemblyBuilder.setField("deployed", deploy);

				ParameterInitializer initializer = new DefaultParameterInitializer(null, ignite, reference, assemblyBuilder);
				Util.invoke(new AssemblyOperation(deploy ? Deploy.class : Undeploy.class, assemblyType.toString()), assemblyCache, initializer, e -> new AssemblyException(e));

				IgniteCache<BinaryObject, BinaryObject> tenantCache = ignite.cache(Tenant.TENANT_CACHE_NAME).withKeepBinary();
				BinaryObject assemblyKey = ignite.binary().builder("AssemblyKey").setField("id", reference.toString()).build();
				tenantCache.put(assemblyKey, assemblyBuilder.build());
				tx.commit();

			}
		} catch (BinaryObjectException | URISyntaxException | IllegalArgumentException e) {
			throw new AssemblyException(e);
		}

	}

	@Override
	public void deploy(URI reference) throws AssemblyException {
		// TODO dependency deployment check
		deploymentUpdate(reference, true);
	}

	@Override
	public void undeploy(URI reference) throws AssemblyException {
		// TODO dependency undeployment check
		deploymentUpdate(reference, false);

	}

	@Override
	public void createAlias(String alias, URI reference) throws AssemblyException {
		try {
			IgniteTransactions transactions = ignite.transactions();
			try (Transaction tx = transactions.txStart()) {
				IgniteCache<BinaryObject, BinaryObject> tenantCache = ignite.cache(Tenant.TENANT_CACHE_NAME).withKeepBinary();

				BinaryObject assemblyAliasKey = ignite.binary().builder("AssemblyAliasKey").setField("alias", alias).build();

				BinaryObjectBuilder assemblyAliasBuilder = ignite.binary().builder("AssemblyAlias");
				assemblyAliasBuilder.setField("oid", UUID.randomUUID());
				assemblyAliasBuilder.setField("id", reference.toString());
				assemblyAliasBuilder.setField("createdTime", ZonedDateTime.now(ZoneId.systemDefault()));
				assemblyAliasBuilder.setField("modifiedTime", ZonedDateTime.now(ZoneId.systemDefault()));

				if (!tenantCache.putIfAbsent(assemblyAliasKey, assemblyAliasBuilder.build())) {
					throw new AssemblyException(String.format("Assembly alias %s already exists", alias));
				}
				tx.commit();

			}
		} catch (BinaryObjectException | IllegalArgumentException e) {
			throw new AssemblyException(e);
		}

	}

	@Override
	public void deleteAlias(String alias) throws AssemblyException {
		try {
			IgniteTransactions transactions = ignite.transactions();
			try (Transaction tx = transactions.txStart()) {
				IgniteCache<BinaryObject, BinaryObject> tenantCache = ignite.cache(Tenant.TENANT_CACHE_NAME).withKeepBinary();

				BinaryObject assemblyAliasKey = ignite.binary().builder("AssemblyAliasKey").setField("alias", alias).build();

				tenantCache.remove(assemblyAliasKey);
				tx.commit();

			}
		} catch (BinaryObjectException | IllegalArgumentException e) {
			throw new AssemblyException(e);
		}

	}

	@Override
	public URI alias(String alias) throws AssemblyException {
		try {
			IgniteCache<BinaryObject, BinaryObject> tenantCache = ignite.cache(Tenant.TENANT_CACHE_NAME).withKeepBinary();

			BinaryObject assemblyAliasKey = ignite.binary().builder("AssemblyAliasKey").setField("alias", alias).build();

			BinaryObject assemblyAlias = tenantCache.get(assemblyAliasKey);
			if (assemblyAlias == null) {
				throw new AssemblyException(String.format("Assembly alias %s does not  exist", alias));
			}
			return new URI(assemblyAlias.field("id"));

		} catch (BinaryObjectException | IllegalArgumentException | URISyntaxException e) {
			throw new AssemblyException(e);
		}
	}

	private static class AssemblyOperation {
		private final Class<?> operation;
		private final String type;
		private final String stage;
		private final UpdateType updateType;

		public AssemblyOperation(Class<?> operation, String type) {
			this.operation = operation;
			this.type = type;
			this.stage = null;
			this.updateType = null;
		}

		public AssemblyOperation(String type, String stage) {
			this.operation = Stage.class;
			this.type = type;
			this.stage = stage;
			this.updateType = null;
		}

		public AssemblyOperation(String type, UpdateType updateType) {
			this.operation = Update.class;
			this.type = type;
			this.updateType = updateType;
			this.stage = null;

		}

		public Class<?> getOperation() {
			return operation;
		}

		public String getType() {
			return type;
		}

		public String getStage() {
			return stage;
		}

		public UpdateType getUpdateType() {
			return updateType;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((operation == null) ? 0 : operation.hashCode());
			result = prime * result + ((stage == null) ? 0 : stage.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			result = prime * result + ((updateType == null) ? 0 : updateType.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			AssemblyOperation other = (AssemblyOperation) obj;
			if (operation == null) {
				if (other.operation != null)
					return false;
			} else if (!operation.equals(other.operation))
				return false;
			if (stage == null) {
				if (other.stage != null)
					return false;
			} else if (!stage.equals(other.stage))
				return false;
			if (type == null) {
				if (other.type != null)
					return false;
			} else if (!type.equals(other.type))
				return false;
			if (updateType != other.updateType)
				return false;
			return true;
		}

	}

	private BinaryObject assemblyTypeConfig(Ignite ignite, String assemblyType) throws AssemblyException {
		IgniteCache<BinaryObject, BinaryObject> configCache = ignite.cache(Tenant.TENANT_CACHE_NAME).withKeepBinary();
		BinaryObject assemblyTypeConfig = configCache.get(Assembler.assemblyTypeConfigPath(ignite, assemblyType));
		if (assemblyTypeConfig != null) {
			return assemblyTypeConfig;
		}
		throw new AssemblyException(String.format("Assembly configuration %s not found", assemblyType));

	}

	private static String getAssemblyType(Method method) throws IllegalArgumentException {
		AssemblyType type = method.getDeclaredAnnotation(AssemblyType.class);
		if (type != null) {
			return type.value();
		}
		type = method.getDeclaringClass().getDeclaredAnnotation(AssemblyType.class);
		if (type != null) {
			return type.value();
		}
		throw new IllegalArgumentException(String.format("Class %s method %s do not have required Type annotation", method.getDeclaringClass(), method.getName()));
	}

	private static int getPriority(Method method) throws IllegalArgumentException {
		Priority priority = method.getDeclaredAnnotation(Priority.class);
		if (priority != null) {
			return priority.value();
		}
		priority = method.getDeclaringClass().getDeclaredAnnotation(Priority.class);
		if (priority != null) {
			return priority.value();
		}
		return 10;
	}

	private static class AssemblyOperationIndexer implements MethodIndexer<Assembler, AssemblyOperation> {

		@Override
		public void index(Assembler instance, Method method, Map<AssemblyOperation, List<Invocation<Assembler>>> methodCache) throws IllegalArgumentException, IllegalAccessException {
			for (Class annotationClass : Set.of(Create.class, Export.class, Update.class, Delete.class, Deploy.class, Undeploy.class, Stage.class)) {
				Annotation annotation = method.getDeclaredAnnotation(annotationClass);
				if (annotation != null) {
					String assemblyType = getAssemblyType(method);
					int priority = getPriority(method);
					AssemblyOperation key = null;
					if (annotation instanceof Stage) {
						key = new AssemblyOperation(assemblyType, ((Stage) annotation).value());
					} else if (annotation instanceof Update) {
						key = new AssemblyOperation(assemblyType, ((Update) annotation).type());
					} else {
						key = new AssemblyOperation(annotationClass, assemblyType);
					}
					List<Invocation<Assembler>> invocations = methodCache.computeIfAbsent(key, k -> new ArrayList<>(1));
					invocations.add(new Invocation<Assembler>(instance, method, priority));
				}
			}

		}

	}

	public static class AssembleContext extends ContextImpl {

		private static AssembleContext INSTANCE = new AssembleContext();
		private ThreadLocal<ThreadLocalState> state = new ThreadLocal<ThreadLocalState>();

		private AssembleContext() {

		}

		public static AssembleContext instance() {
			return INSTANCE;
		}

		@Override
		public Class<? extends Annotation> getScope() {
			return AssembleScoped.class;
		}

		@Override
		protected ThreadLocal<ThreadLocalState> state() {
			return state;
		}

	}

}
