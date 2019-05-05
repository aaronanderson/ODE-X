package org.apache.ode.runtime.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
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
import org.apache.ignite.igfs.IgfsPath;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.transactions.Transaction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.ode.spi.CDIService;
import org.apache.ode.spi.deployment.Assembly;
import org.apache.ode.spi.deployment.Assembly.AssemblyException;
import org.apache.ode.spi.deployment.Assembly.Id;
import org.apache.ode.spi.deployment.AssemblyManager;
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
	public <C> void create(URI type, URI reference, C config, Entry... files) throws AssemblyException {
		Assembly assembly = Optional.ofNullable(assemblyCache.get(type)).orElseThrow(() -> new AssemblyException(String.format("Assembly type %s is unavailable", type)));
		IgniteCache<BinaryObject, BinaryObject> tenantCache = ignite.cache(Tenant.TENANT_CACHE_NAME).withKeepBinary();
		BinaryObject assemblyKey = ignite.binary().builder("AssemblyKey").setField("id", reference.toString()).build();
		IgniteTransactions transactions = ignite.transactions();
		try (Transaction tx = transactions.txStart()) {
			UUID oid = UUID.randomUUID();
			BinaryObjectBuilder assemblyBuilder = ignite.binary().builder("Assembly");
			assemblyBuilder.setField("oid", oid);
			assemblyBuilder.setField("type", type.toString());
			assemblyBuilder.setField("createdTime", ZonedDateTime.now(ZoneId.systemDefault()));
			assemblyBuilder.setField("modifiedTime", ZonedDateTime.now(ZoneId.systemDefault()));
			if (!tenantCache.putIfAbsent(assemblyKey, assemblyBuilder.build())) {
				throw new AssemblyException(String.format("Assembly %s already exists", reference));
			}

			if (files.length > 0) {
				IgniteFileSystem repositoryFS = ignite.fileSystem(Tenant.REPOSITORY_FILE_SYSTEM);
				IgfsPath assemblyPath = new IgfsPath(ASSEMBLY_DIR, oid.toString());
				repositoryFS.mkdirs(assemblyPath);
				for (Entry entry : files) {
					IgfsPath filePath = null;
					if (entry.getPath().contains("/")) {
						int endPathIndex = entry.getPath().lastIndexOf("/");
						IgfsPath parentPath = new IgfsPath(assemblyPath, entry.getPath().substring(0, endPathIndex));
						repositoryFS.mkdirs(parentPath);
						filePath = new IgfsPath(parentPath, entry.getPath().substring(endPathIndex + 1));
					} else {
						filePath = assemblyPath.suffix(entry.getPath());
					}

					try (InputStream is = entry.getLocation().openStream(); OutputStream os = repositoryFS.create(filePath, false);) {
						is.transferTo(os);
					} catch (IOException e) {
						throw new AssemblyException(e);
					}
				}
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

	@Override
	public <C> void update(URI reference, C file) {
		// TODO Auto-generated method stub

	}

	@Override
	public void update(URI reference, Entry... files) {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(URI reference) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deploy(URI reference) {
		// TODO Auto-generated method stub

	}

	@Override
	public void undeploy(URI reference) {
		// TODO Auto-generated method stub

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
