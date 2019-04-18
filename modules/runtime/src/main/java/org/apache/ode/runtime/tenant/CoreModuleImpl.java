package org.apache.ode.runtime.tenant;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.binary.BinaryObjectBuilder;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.QueryEntity;
import org.apache.ignite.cache.QueryIndex;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ode.spi.tenant.Module;
import org.apache.ode.spi.tenant.Tenant;
import org.apache.ode.spi.tenant.Module.Id;

@Id(CoreModuleImpl.CORE_MODULE_ID)
public class CoreModuleImpl implements Module {

	public static final String CORE_MODULE_ID = "org:apache:ode:core";

	@Inject
	Ignite ignite;

	@Enable
	public void enable(Ignite ignite) {
		// set IGNITE_H2_DEBUG_CONSOLE=test environment variable to launch embedded H2 DB explorer web application.
		if (ignite.cache(Tenant.TENANT_CACHE_NAME) == null) {
			CacheConfiguration tenantCacheCfg = new CacheConfiguration(Tenant.TENANT_CACHE_NAME);
			tenantCacheCfg.setSqlSchema("ODE");
			tenantCacheCfg.setCacheMode(CacheMode.REPLICATED);
			tenantCacheCfg.setSqlIndexMaxInlineSize(120);
			List<QueryEntity> entities = new LinkedList<>();
			entities.add(createConfiguration());
			entities.add(createEndpoint());
			entities.add(createAssembly());
			entities.add(createComposite());
			tenantCacheCfg.setQueryEntities(entities);
			tenantCacheCfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
			IgniteCache<UUID, BinaryObject> configCache = ignite.createCache(tenantCacheCfg);

			BinaryObjectBuilder modules = ignite.binary().builder("Configuration");
			modules.setField("type", "modules");
			modules.setField("path", "/modules");
			modules.setField("modifiedTime", ZonedDateTime.now(ZoneId.systemDefault()));
			configCache.put(UUID.randomUUID(), modules.build());

			BinaryObjectBuilder tenant = ignite.binary().builder("Configuration");
			tenant.setField("type", "tenant");
			tenant.setField("path", "/tenant");
			tenant.setField("modifiedTime", ZonedDateTime.now(ZoneId.systemDefault()));
			tenant.setField("online", false);
			configCache.put(UUID.randomUUID(), tenant.build());
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

	}

	@Disable
	public void disable(Ignite ignite) {
		ignite.destroyCache("Tenant");
	}

	private QueryEntity createConfiguration() {
		QueryEntity entity = new QueryEntity(UUID.class.getName(), "Configuration");
		entity.setTableName("CONFIGURATION");

		entity.setKeyType(UUID.class.getName());
		entity.setKeyFieldName("oid");

		LinkedHashMap<String, String> efields = new LinkedHashMap<>();
		efields.put("oid", UUID.class.getName());
		efields.put("type", String.class.getName());
		efields.put("path", String.class.getName());
		efields.put("createdTime", Timestamp.class.getName());
		efields.put("modifiedTime", Timestamp.class.getName());
		efields.put("metadata", byte[].class.getName());
		efields.put("value", byte[].class.getName());

		entity.setFields(efields);

		Collection<QueryIndex> eindexes = new ArrayList<>(1);
		eindexes.add(new QueryIndex("path"));
		eindexes.add(new QueryIndex("type"));

		entity.setIndexes(eindexes);

		return entity;
	}

	private QueryEntity createProcess() {
		QueryEntity entity = new QueryEntity(UUID.class.getName(), "Process");
		entity.setTableName("PROCESS");

		entity.setKeyType(UUID.class.getName());
		entity.setKeyFieldName("oid");

		LinkedHashMap<String, String> efields = new LinkedHashMap<>();
		efields.put("oid", UUID.class.getName());
		efields.put("parentOid", UUID.class.getName());
		efields.put("processId", String.class.getName());
		efields.put("type", String.class.getName());
		efields.put("createdTime", Timestamp.class.getName());
		efields.put("modifiedTime", Timestamp.class.getName());
		efields.put("entry", byte[].class.getName());

		entity.setFields(efields);

		Collection<QueryIndex> eindexes = new ArrayList<>(1);
		eindexes.add(new QueryIndex("processId"));
		eindexes.add(new QueryIndex("parentOid"));
		eindexes.add(new QueryIndex("type"));

		entity.setIndexes(eindexes);

		return entity;
	}

	private QueryEntity createAssembly() {
		QueryEntity entity = new QueryEntity(UUID.class.getName(), "Assembly");
		entity.setTableName("ASSEMBLY");

		entity.setKeyType(UUID.class.getName());
		entity.setKeyFieldName("oid");

		LinkedHashMap<String, String> efields = new LinkedHashMap<>();
		efields.put("oid", UUID.class.getName());
		efields.put("type", String.class.getName());
		efields.put("createdTime", Timestamp.class.getName());
		efields.put("modifiedTime", Timestamp.class.getName());
		efields.put("entry", byte[].class.getName());

		entity.setFields(efields);

		Collection<QueryIndex> eindexes = new ArrayList<>(1);
		eindexes.add(new QueryIndex("type"));

		entity.setIndexes(eindexes);

		return entity;
	}

	private QueryEntity createComposite() {
		QueryEntity entity = new QueryEntity(UUID.class.getName(), "Composite");
		entity.setTableName("COMPOSITE");

		entity.setKeyType(UUID.class.getName());
		entity.setKeyFieldName("oid");

		LinkedHashMap<String, String> efields = new LinkedHashMap<>();
		efields.put("oid", UUID.class.getName());
		efields.put("type", String.class.getName());
		efields.put("createdTime", Timestamp.class.getName());
		efields.put("modifiedTime", Timestamp.class.getName());
		efields.put("entry", byte[].class.getName());

		entity.setFields(efields);

		Collection<QueryIndex> eindexes = new ArrayList<>(1);
		eindexes.add(new QueryIndex("type"));

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
