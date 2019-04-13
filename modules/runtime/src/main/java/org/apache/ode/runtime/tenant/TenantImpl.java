package org.apache.ode.runtime.tenant;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.cache.Cache.Entry;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.binary.BinaryObjectBuilder;
import org.apache.ignite.cache.query.SqlQuery;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.services.ServiceContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.ode.spi.CDIService;
import org.apache.ode.spi.config.Config;
import org.apache.ode.spi.tenant.Module;
import org.apache.ode.spi.tenant.Module.ModuleException;
import org.apache.ode.spi.tenant.Module.ModuleStatus;
import org.apache.ode.spi.tenant.Tenant;

@ApplicationScoped
public class TenantImpl extends CDIService implements Tenant {

	public static final Logger LOG = LogManager.getLogger(TenantImpl.class);

	@Inject
	@Any
	private transient Instance<Module> modules;

	@Inject
	transient Config odeConfig;

	@IgniteInstanceResource
	private Ignite ignite;

	private TenantStatus tenantStatus = TenantStatus.OFFLINE;

	// private IgniteCountDownLatch startupLatch;
	private CountDownLatch startupLatch = new CountDownLatch(1);

	@Override
	public Set<String> modules() {
		return modules.stream().map(m -> m.id()).collect(Collectors.toSet());
	}

	private <M extends Module> M getModule(String moduleId) throws ModuleException {
		return (M) modules.stream().filter(m -> m.id().equals(moduleId)).findFirst().orElseThrow(() -> new ModuleException(String.format("Module %s not found", moduleId)));
	}

	@Override
	public <M extends Module> M instance(String moduleId) throws ModuleException {
		return getModule(moduleId);
	}

	@Override
	public ModuleStatus status(String moduleId) throws ModuleException {
		getModule(moduleId);
		BinaryObject moduleConfig = getOrCreateModuleConfig(moduleId);
		return (Boolean) moduleConfig.field("enabled") ? ModuleStatus.ENABLED : ModuleStatus.DISABLED;
	}

	@Override
	public void enable(String moduleId) throws ModuleException {
		Module m = getModule(moduleId);
		BinaryObject moduleConfig = getOrCreateModuleConfig(moduleId);
		Boolean enabled = moduleConfig.field("enabled");
		if (!enabled) {
			m.enable();
			BinaryObjectBuilder moduleConfigBuilder = ignite.binary().builder(moduleConfig);
			moduleConfigBuilder.setField("modifiedTime", ZonedDateTime.now(ZoneId.systemDefault()));
			moduleConfigBuilder.setField("enabled", true);
			IgniteCache<UUID, BinaryObject> configCache = ignite.cache(CoreModuleImpl.TENANT_CACHE_NAME);
			configCache.put(UUID.randomUUID(), moduleConfig);
		}
	}

	@Override
	public void disable(String moduleId) throws ModuleException {
		Module m = getModule(moduleId);
		BinaryObject moduleConfig = getOrCreateModuleConfig(moduleId);
		Boolean enabled = moduleConfig.field("enabled");
		if (enabled) {
			m.disable();
			BinaryObjectBuilder moduleConfigBuilder = ignite.binary().builder(moduleConfig);
			moduleConfigBuilder.setField("modifiedTime", ZonedDateTime.now(ZoneId.systemDefault()));
			moduleConfigBuilder.setField("enabled", false);
			IgniteCache<UUID, BinaryObject> configCache = ignite.cache(CoreModuleImpl.TENANT_CACHE_NAME);
			configCache.put(UUID.randomUUID(), moduleConfig);
		}
	}

	@Override
	public void init(ServiceContext ctx) throws Exception {
		super.init(ctx);
	}

	@Override
	public TenantStatus status() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void awaitInitialization(long timeout, TimeUnit unit) {
		if (tenantStatus == TenantStatus.OFFLINE) {
			try {
				startupLatch.await(timeout, unit);
			} catch (InterruptedException e) {
				LOG.error("await interrupted", e);
			}
		}
	}

	// initialize tenant in execute operation since initialization in init operation can cause Ignite kernel deadlock (deployed service waiting on uninitialized kernel services)
	@Override
	public void execute(ServiceContext ctx) throws Exception {
		// startupLatch = ignite.countDownLatch(Tenant.SERVICE_NAME + ":startup", 1, true, true);

		if (odeConfig.getBool("ode.auto-enable").orElse(false)) {
			autoEnable();

		}
		// IgniteMessaging msg = ignite.message();
		// msg.remoteListen(TOPIC, new TenantActionListener());
		tenantStatus = TenantStatus.ONLINE;
		startupLatch.countDown();

	}

	private void autoEnable() throws Exception {

		IgniteCache<BinaryObject, BinaryObject> tenantCache = ignite.cache(CoreModuleImpl.TENANT_CACHE_NAME);
		if (tenantCache == null) {
			CoreModuleImpl coreModule = (CoreModuleImpl) modules.stream().filter(m -> CoreModuleImpl.CORE_MODULE_ID.equals(m.id())).findFirst().orElseThrow(() -> new ModuleException("Core module not found"));
			coreModule.enable();
			createModuleConfig(CoreModuleImpl.CORE_MODULE_ID, true);
		}

		Set<String> resolved = new HashSet<>();

		// check configuration to see which modules are already enabled

	}

	private BinaryObject getOrCreateModuleConfig(String moduleId) {
		IgniteCache<UUID, BinaryObject> configCache = ignite.cache(CoreModuleImpl.TENANT_CACHE_NAME);
		SqlQuery<UUID, BinaryObject> query = new SqlQuery<>("Configuration", "path = ?");
		List<Entry<UUID, BinaryObject>> existing = configCache.query(query.setArgs(0, modulePath(moduleId))).getAll();
		if (!existing.isEmpty()) {
			return existing.get(0).getValue();
		}
		return createModuleConfig(moduleId, false);

	}

	private BinaryObject createModuleConfig(String moduleId, boolean enabled) {
		IgniteCache<UUID, BinaryObject> configCache = ignite.cache(CoreModuleImpl.TENANT_CACHE_NAME);
		BinaryObjectBuilder moduleConfigBuilder = ignite.binary().builder("Configuration");
		moduleConfigBuilder.setField("key", "modules");
		moduleConfigBuilder.setField("path", modulePath(moduleId));
		moduleConfigBuilder.setField("modifiedTime", ZonedDateTime.now(ZoneId.systemDefault()));
		moduleConfigBuilder.setField("enabled", enabled);
		BinaryObject moduleConfig = moduleConfigBuilder.build();
		configCache.put(UUID.randomUUID(), moduleConfig);
		return moduleConfig;
	}

	private String modulePath(String moduleId) {
		return "/modules/" + moduleId;
	}

}
