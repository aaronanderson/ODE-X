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
import org.apache.ignite.IgniteCluster;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.binary.BinaryObjectBuilder;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.QueryEntity;
import org.apache.ignite.cache.QueryIndex;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ode.spi.tenant.ClusterManager;
import org.apache.ode.spi.tenant.Module;
import org.apache.ode.spi.tenant.Module.Id;

@Id(CoreModuleImpl.CORE_MODULE_ID)
public class CoreModuleImpl implements Module, ClusterManager {

	public static final String CORE_MODULE_ID = "org:apache:ode:core";
	public static final String TENANT_CACHE_NAME = "Tenant";
	public static final String PROCESS_CACHE_NAME = "Process";

	@Inject
	Ignite ignite;

	@Enable
	public void enable(Ignite ignite) {
		// set IGNITE_H2_DEBUG_CONSOLE=test environment variable to launch embedded H2 DB explorer web application.
		if (ignite.cache(TENANT_CACHE_NAME) == null) {
			CacheConfiguration tenantCacheCfg = new CacheConfiguration(TENANT_CACHE_NAME);
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

			BinaryObjectBuilder repositories = ignite.binary().builder("Configuration");
			repositories.setField("key", "modules");
			repositories.setField("path", "/modules");
			repositories.setField("modifiedTime", ZonedDateTime.now(ZoneId.systemDefault()));
			configCache.put(UUID.randomUUID(), repositories.build());
		}
		if (ignite.cache(PROCESS_CACHE_NAME) == null) {
			CacheConfiguration tenantCacheCfg = new CacheConfiguration(PROCESS_CACHE_NAME);
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
		efields.put("key", String.class.getName());
		efields.put("path", String.class.getName());
		efields.put("createdTime", Timestamp.class.getName());
		efields.put("modifiedTime", Timestamp.class.getName());
		efields.put("metadata", byte[].class.getName());
		efields.put("value", byte[].class.getName());

		entity.setFields(efields);

		Collection<QueryIndex> eindexes = new ArrayList<>(1);
		eindexes.add(new QueryIndex("path"));

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

	@Override
	public void activate(boolean value) {
		IgniteCluster igniteCluster = ignite.cluster();
		igniteCluster.active(true);
		igniteCluster.setBaselineTopology(1l);
		Collection<ClusterNode> nodes = ignite.cluster().forServers().nodes();
		igniteCluster.setBaselineTopology(nodes);

	}

	@Override
	public void baselineTopology(long version) {
		IgniteCluster igniteCluster = ignite.cluster();
		igniteCluster.setBaselineTopology(1l);
		Collection<ClusterNode> nodes = ignite.cluster().forServers().nodes();
		igniteCluster.setBaselineTopology(nodes);

	}

}
