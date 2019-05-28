package org.apache.ode.runtime.tenant;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteAtomicSequence;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteFileSystem;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.binary.BinaryObjectBuilder;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.CacheRebalanceMode;
import org.apache.ignite.cache.QueryEntity;
import org.apache.ignite.cache.QueryIndex;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ode.runtime.deployment.AssemblyManagerImpl;
import org.apache.ode.runtime.deployment.CompositeManagerImpl;
import org.apache.ode.spi.deployment.AssemblyManager;
import org.apache.ode.spi.deployment.CompositeManager;
import org.apache.ode.spi.tenant.Module;
import org.apache.ode.spi.tenant.Module.Id;
import org.apache.ode.spi.tenant.Tenant;

@Id(CoreModule.CORE_MODULE_ID)
public class CoreModule implements Module {

	public static final String CORE_MODULE_ID = "org:apache:ode:core";

	@Enable
	public void enable(Ignite ignite) {
		// set IGNITE_H2_DEBUG_CONSOLE=test environment variable to launch embedded H2 DB explorer web application.
		if (ignite.cache(Tenant.TENANT_CACHE_NAME) == null) {
			CacheConfiguration tenantCacheCfg = new CacheConfiguration(Tenant.TENANT_CACHE_NAME);
			tenantCacheCfg.setSqlSchema("ODE");
			tenantCacheCfg.setCacheMode(CacheMode.REPLICATED);
			tenantCacheCfg.setRebalanceMode(CacheRebalanceMode.SYNC);
			tenantCacheCfg.setSqlIndexMaxInlineSize(120);
			List<QueryEntity> entities = new LinkedList<>();
			entities.add(createConfiguration());
			entities.add(createEndpoint());
			entities.add(createAssembly());
			entities.add(createAssemblyAlias());
			entities.add(createComposite());
			entities.add(createCompositeAlias());
			tenantCacheCfg.setQueryEntities(entities);
			tenantCacheCfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
			IgniteCache<BinaryObject, BinaryObject> configCache = ignite.createCache(tenantCacheCfg).withKeepBinary();

			BinaryObjectBuilder modulesKey = ignite.binary().builder("ConfigurationKey");
			modulesKey.setField("path", "/ode:modules");
			BinaryObjectBuilder modules = ignite.binary().builder("Configuration");
			modules.setField("oid", UUID.randomUUID());
			modules.setField("type", "ode:modules");
			modules.setField("modifiedTime", ZonedDateTime.now(ZoneId.systemDefault()));
			configCache.put(modulesKey.build(), modules.build());

			BinaryObjectBuilder assembliesKey = ignite.binary().builder("ConfigurationKey");
			assembliesKey.setField("path", "/ode:assemblies");
			BinaryObjectBuilder assemblies = ignite.binary().builder("Configuration");
			assemblies.setField("oid", UUID.randomUUID());
			assemblies.setField("type", "ode:assemblies");
			assemblies.setField("modifiedTime", ZonedDateTime.now(ZoneId.systemDefault()));
			configCache.put(assembliesKey.build(), assemblies.build());
			
			BinaryObjectBuilder contentTypesKey = ignite.binary().builder("ConfigurationKey");
			assembliesKey.setField("path", "/ode:contentTypes");
			BinaryObjectBuilder contentTypes = ignite.binary().builder("Configuration");
			assemblies.setField("oid", UUID.randomUUID());
			assemblies.setField("type", "ode:contentTypes");
			assemblies.setField("modifiedTime", ZonedDateTime.now(ZoneId.systemDefault()));
			configCache.put(contentTypesKey.build(), contentTypes.build());

			BinaryObjectBuilder tenantKey = ignite.binary().builder("ConfigurationKey");
			tenantKey.setField("path", "/ode:tenant");
			BinaryObjectBuilder tenant = ignite.binary().builder("Configuration");
			tenant.setField("oid", UUID.randomUUID());
			tenant.setField("type", "ode:tenant");
			tenant.setField("modifiedTime", ZonedDateTime.now(ZoneId.systemDefault()));
			tenant.setField("online", false);
			tenant.setField("endpointIdKey", new Random().nextInt());
			configCache.put(tenantKey.build(), tenant.build());
		}
		if (ignite.cache(Tenant.PROCESS_CACHE_NAME) == null) {
			CacheConfiguration tenantCacheCfg = new CacheConfiguration(Tenant.PROCESS_CACHE_NAME);
			tenantCacheCfg.setSqlSchema("ODE");
			tenantCacheCfg.setCacheMode(CacheMode.PARTITIONED);
			tenantCacheCfg.setSqlIndexMaxInlineSize(120);
			List<QueryEntity> entities = new LinkedList<>();
			entities.add(createProcess());
			tenantCacheCfg.setQueryEntities(entities);
			tenantCacheCfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
			ignite.createCache(tenantCacheCfg);
		}

		IgniteFileSystem repositoryFS = ignite.fileSystem(Tenant.REPOSITORY_FILE_SYSTEM);
		repositoryFS.mkdirs(AssemblyManager.ASSEMBLY_DIR);
		repositoryFS.mkdirs(CompositeManager.COMPOSITE_DIR);

		ignite.services().deployNodeSingleton(AssemblyManager.SERVICE_NAME, new AssemblyManagerImpl());
		ignite.services().deployNodeSingleton(CompositeManager.SERVICE_NAME, new CompositeManagerImpl());

		IgniteAtomicSequence seq = ignite.atomicSequence("endpointId", 0, true);

		// seq.incrementAndGet()
	}

	@Disable
	public void disable(Ignite ignite) {
		ignite.destroyCache(Tenant.TENANT_CACHE_NAME);
		ignite.destroyCache(Tenant.PROCESS_CACHE_NAME);
		IgniteFileSystem repositoryFS = ignite.fileSystem(Tenant.REPOSITORY_FILE_SYSTEM);
		repositoryFS.delete(AssemblyManager.ASSEMBLY_DIR, true);
		repositoryFS.delete(CompositeManager.COMPOSITE_DIR, true);

	}

	private QueryEntity createConfiguration() {
		QueryEntity entity = new QueryEntity("ConfigurationKey", "Configuration");
		entity.setTableName("CONFIGURATION");

		LinkedHashMap<String, String> efields = new LinkedHashMap<>();
		efields.put("oid", UUID.class.getName());
		efields.put("type", String.class.getName());
		efields.put("path", String.class.getName());
		efields.put("createdTime", Timestamp.class.getName());
		efields.put("modifiedTime", Timestamp.class.getName());
		efields.put("metadata", byte[].class.getName());
		efields.put("value", byte[].class.getName());

		entity.setFields(efields);

		Set<String> ekfields = new HashSet<>();
		ekfields.add("path");
		entity.setKeyFields(ekfields);

		Collection<QueryIndex> eindexes = new ArrayList<>(1);
		eindexes.add(new QueryIndex("oid"));
		eindexes.add(new QueryIndex("type"));

		entity.setIndexes(eindexes);

		return entity;
	}

	private QueryEntity createProcess() {
		QueryEntity entity = new QueryEntity("ProcessKey", "Process");
		entity.setTableName("PROCESS");

		LinkedHashMap<String, String> efields = new LinkedHashMap<>();
		efields.put("processId", String.class.getName());
		efields.put("oid", UUID.class.getName());
		efields.put("parentOid", UUID.class.getName());
		efields.put("type", String.class.getName());
		efields.put("createdTime", Timestamp.class.getName());
		efields.put("modifiedTime", Timestamp.class.getName());
		efields.put("entry", byte[].class.getName());

		entity.setFields(efields);

		Set<String> ekfields = new HashSet<>();
		ekfields.add("processId");
		entity.setKeyFields(ekfields);

		Collection<QueryIndex> eindexes = new ArrayList<>(1);
		eindexes.add(new QueryIndex("oid"));
		eindexes.add(new QueryIndex("parentOid"));
		eindexes.add(new QueryIndex("type"));

		entity.setIndexes(eindexes);

		return entity;
	}

	private QueryEntity createAssembly() {
		QueryEntity entity = new QueryEntity("AssemblyKey", "Assembly");
		entity.setTableName("ASSEMBLY");

		LinkedHashMap<String, String> efields = new LinkedHashMap<>();
		efields.put("id", String.class.getName());
		efields.put("oid", UUID.class.getName());
		efields.put("type", String.class.getName());
		efields.put("dependencies", String[].class.getName());
		efields.put("createdTime", Timestamp.class.getName());
		efields.put("modifiedTime", Timestamp.class.getName());
		efields.put("deployed", Boolean.class.getName());
		efields.put("entry", byte[].class.getName());
		efields.put("stage", String.class.getName());

		entity.setFields(efields);

		Set<String> ekfields = new HashSet<>();
		ekfields.add("id");
		entity.setKeyFields(ekfields);

		Collection<QueryIndex> eindexes = new ArrayList<>(1);
		eindexes.add(new QueryIndex("oid"));
		eindexes.add(new QueryIndex("type"));

		entity.setIndexes(eindexes);

		return entity;
	}

	private QueryEntity createAssemblyAlias() {
		QueryEntity entity = new QueryEntity("AssemblyAliasKey", "AssemblyAlias");
		entity.setTableName("ASSEMBLY_ALIAS");

		LinkedHashMap<String, String> efields = new LinkedHashMap<>();
		efields.put("alias", String.class.getName());
		efields.put("id", String.class.getName());
		efields.put("oid", UUID.class.getName());
		efields.put("createdTime", Timestamp.class.getName());
		efields.put("modifiedTime", Timestamp.class.getName());

		entity.setFields(efields);

		Set<String> ekfields = new HashSet<>();
		ekfields.add("alias");
		entity.setKeyFields(ekfields);

		Collection<QueryIndex> eindexes = new ArrayList<>(1);
		eindexes.add(new QueryIndex("oid"));
		eindexes.add(new QueryIndex("id"));

		entity.setIndexes(eindexes);

		return entity;
	}	

	private QueryEntity createComposite() {
		QueryEntity entity = new QueryEntity("CompositeKey", "Composite");
		entity.setTableName("COMPOSITE");

		LinkedHashMap<String, String> efields = new LinkedHashMap<>();
		efields.put("id", String.class.getName());
		efields.put("oid", UUID.class.getName());
		efields.put("type", String.class.getName());
		efields.put("createdTime", Timestamp.class.getName());
		efields.put("modifiedTime", Timestamp.class.getName());
		efields.put("entry", byte[].class.getName());

		entity.setFields(efields);

		Set<String> ekfields = new HashSet<>();
		ekfields.add("id");
		entity.setKeyFields(ekfields);

		Collection<QueryIndex> eindexes = new ArrayList<>(1);
		eindexes.add(new QueryIndex("oid"));
		eindexes.add(new QueryIndex("type"));

		entity.setIndexes(eindexes);

		return entity;
	}

	private QueryEntity createCompositeAlias() {
		QueryEntity entity = new QueryEntity("CompositeAliasKey", "CompositeAlias");
		entity.setTableName("COMPOSITE_ALIAS");

		LinkedHashMap<String, String> efields = new LinkedHashMap<>();
		efields.put("alias", String.class.getName());
		efields.put("id", String.class.getName());
		efields.put("oid", UUID.class.getName());
		efields.put("createdTime", Timestamp.class.getName());
		efields.put("modifiedTime", Timestamp.class.getName());

		entity.setFields(efields);

		Set<String> ekfields = new HashSet<>();
		ekfields.add("alias");
		entity.setKeyFields(ekfields);

		Collection<QueryIndex> eindexes = new ArrayList<>(1);
		eindexes.add(new QueryIndex("oid"));
		eindexes.add(new QueryIndex("id"));

		entity.setIndexes(eindexes);

		return entity;
	}

	private QueryEntity createEndpoint() {
		QueryEntity entity = new QueryEntity(UUID.class.getName(), "Endpoint");
		entity.setTableName("ENDPOINT");

		entity.setKeyType(UUID.class.getName());
		entity.setKeyFieldName("oid");

		LinkedHashMap<String, String> efields = new LinkedHashMap<>();
		efields.put("oid", UUID.class.getName());
		efields.put("compositeId", UUID.class.getName());
		efields.put("type", String.class.getName());
		efields.put("createdTime", Timestamp.class.getName());
		efields.put("modifiedTime", Timestamp.class.getName());
		efields.put("entry", byte[].class.getName());

		entity.setFields(efields);

		Collection<QueryIndex> eindexes = new ArrayList<>(1);
		eindexes.add(new QueryIndex("type"));
		eindexes.add(new QueryIndex("compositeId"));

		entity.setIndexes(eindexes);

		return entity;
	}

}
