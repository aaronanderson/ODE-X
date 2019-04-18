package org.apache.ode.runtime.tenant;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.cache.Cache.Entry;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteMessaging;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.binary.BinaryObjectBuilder;
import org.apache.ignite.cache.query.SqlQuery;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.services.ServiceContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.ode.runtime.Configurator;
import org.apache.ode.spi.CDIService;
import org.apache.ode.spi.CacheEntryImpl;
import org.apache.ode.spi.config.Config;
import org.apache.ode.spi.tenant.Module;
import org.apache.ode.spi.tenant.Module.Disable;
import org.apache.ode.spi.tenant.Module.Enable;
import org.apache.ode.spi.tenant.Module.Id;
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

	// private IgniteCountDownLatch startupLatch;
	private CountDownLatch startupLatch = new CountDownLatch(1);

	private UUID tenantConfigKey;

	@Override
	public String name() {
		return (String) ignite.configuration().getUserAttributes().get(Configurator.ODE_TENANT);
	}

	@Override
	public Set<String> modules() {
		HashSet<String> ids = new HashSet<>();
		for (Module m : modules) {
			try {
				ids.add(getId(m));
			} catch (ModuleException e) {
				LOG.error(String.format("Module %s does not have required Id annotation", m.getClass()));
			}
		}
		return ids;
	}

	private static String getId(Module module) throws ModuleException {
		Id id = Optional.ofNullable(module.getClass().getAnnotation(Id.class)).orElseThrow(() -> new ModuleException(String.format("Module %s does not have required Id annotation", module.getClass())));
		return id.value();
	}

	private static String[] getDependencies(Module module) throws ModuleException {
		Id id = Optional.ofNullable(module.getClass().getAnnotation(Id.class)).orElseThrow(() -> new ModuleException(String.format("Module %s does not have required Id annotation", module.getClass())));
		return id.dependencies();
	}

	public void invoke(Module module, Class clazz, Class annotation, UUID configKey) throws ModuleException {
		if (Object.class.equals(clazz)) {
			return;
		}
		for (Method method : clazz.getDeclaredMethods()) {
			Object enable = method.getDeclaredAnnotation(annotation);
			if (enable != null) {
				invoke(module, configKey, method);
			}
		}
		if (null != clazz.getSuperclass()) {
			invoke(module, clazz.getSuperclass(), annotation, configKey);
		}

	}

	private void invoke(Module module, UUID configKey, Method method) throws ModuleException {
		Object[] args = new Object[method.getParameterCount()];
		for (int i = 0; i < method.getParameterCount(); i++) {
			if (Ignite.class.equals(method.getParameterTypes()[i])) {
				args[i] = ignite;
			} else if (Config.class.equals(method.getParameterTypes()[i])) {
				args[i] = odeConfig;
			} else if (UUID.class.equals(method.getParameterTypes()[i])) {
				args[i] = configKey;
			}
		}
		try {
			method.invoke(module, args);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new ModuleException(e);
		}
	}

	private void enable(Module module, UUID configKey) throws ModuleException {
		invoke(module, module.getClass(), Enable.class, configKey);
	}

	private void disable(Module module, UUID configKey) throws ModuleException {
		invoke(module, module.getClass(), Disable.class, configKey);
	}

	private <M extends Module> M getModule(String moduleId) throws ModuleException {
		for (Module m : modules) {
			try {
				if (moduleId.equals(getId(m))) {
					return (M) m;
				}
			} catch (ModuleException e) {
				LOG.error(String.format("Module %s does not have required Id annotation", m.getClass()));
			}
		}

		throw new ModuleException(String.format("Module %s not found", moduleId));
	}

	@Override
	public ModuleStatus status(String moduleId) throws ModuleException {
		if (tenantCacheAvailable()) {
			getModule(moduleId);
			Entry<UUID, BinaryObject> moduleConfig = getOrCreateModuleConfig(moduleId);
			return (Boolean) moduleConfig.getValue().field("enabled") ? ModuleStatus.ENABLED : ModuleStatus.DISABLED;
		}
		return ModuleStatus.DISABLED;
	}

	@Override
	public synchronized void enable(String moduleId) throws ModuleException {
		if (ALL_MODULES.equals(moduleId)) {
			autoEnable();
		} else {
			Module m = getModule(moduleId);
			Entry<UUID, BinaryObject> moduleConfig = getOrCreateModuleConfig(moduleId);
			enable(m, moduleConfig);
		}
	}

	private void enable(Module module, Entry<UUID, BinaryObject> moduleConfig) throws ModuleException {
		Boolean enabled = moduleConfig.getValue().field("enabled");
		if (!enabled) {
			enable(module, moduleConfig.getKey());
			BinaryObjectBuilder moduleConfigBuilder = ignite.binary().builder(moduleConfig.getValue());
			moduleConfigBuilder.setField("modifiedTime", ZonedDateTime.now(ZoneId.systemDefault()));
			moduleConfigBuilder.setField("enabled", true);
			IgniteCache<UUID, BinaryObject> configCache = ignite.cache(TENANT_CACHE_NAME).withKeepBinary();
			configCache.put(moduleConfig.getKey(), moduleConfigBuilder.build());
			LOG.info("Module {} enabled", getId(module));
		}
	}

	@Override
	public synchronized void disable(String moduleId) throws ModuleException {
		if (!tenantCacheAvailable()) {
			throw new ModuleException("core module not enabled");
		}
		if (ALL_MODULES.equals(moduleId)) {
			autoDisable();
		} else {
			Module m = getModule(moduleId);
			Entry<UUID, BinaryObject> moduleConfig = getOrCreateModuleConfig(moduleId);
			disable(m, moduleConfig);
		}
	}

	private void disable(Module module, Entry<UUID, BinaryObject> moduleConfig) throws ModuleException {
		Boolean enabled = moduleConfig.getValue().field("enabled");
		if (enabled) {
			disable(module, moduleConfig.getKey());
			// Core module disable deletes the cache, can't update
			if (!CoreModuleImpl.CORE_MODULE_ID.equals(getId(module))) {
				BinaryObjectBuilder moduleConfigBuilder = ignite.binary().builder(moduleConfig.getValue());
				moduleConfigBuilder.setField("modifiedTime", ZonedDateTime.now(ZoneId.systemDefault()));
				moduleConfigBuilder.setField("enabled", false);
				IgniteCache<UUID, BinaryObject> configCache = ignite.cache(TENANT_CACHE_NAME).withKeepBinary();
				configCache.put(moduleConfig.getKey(), moduleConfigBuilder.build());
			}
		}

	}

	@Override
	public void init(ServiceContext ctx) throws Exception {
		super.init(ctx);
	}

	@Override
	public TenantStatus status() {
		if (tenantCacheAvailable()) {
			BinaryObject config = getTenantStatusConfig();
			return getTenantStatus(config);
		}
		return TenantStatus.OFFLINE;
	}

	@Override
	public synchronized void status(TenantStatus status) {
		if (tenantCacheAvailable()) {
			BinaryObject config = getTenantStatusConfig();
			TenantStatus currentStatus = getTenantStatus(config);
			if (currentStatus != status) {
				BinaryObjectBuilder newConfig = ignite.binary().builder(config);
				newConfig.setField("modifiedTime", ZonedDateTime.now(ZoneId.systemDefault()));
				newConfig.setField("online", status == TenantStatus.ONLINE);
				IgniteCache<UUID, BinaryObject> configCache = ignite.cache(TENANT_CACHE_NAME).withKeepBinary();
				configCache.put(tenantConfigKey, newConfig.build());
				IgniteMessaging tenantStatusMsg = ignite.message();
				tenantStatusMsg.sendOrdered(STATUS_TOPIC, status, 0);
			}
		} else {
			throw new IllegalStateException("Core Module not enabled, tenant status cannot be persisted");
		}

	}

	@Override
	public void awaitInitialization(long timeout, TimeUnit unit) throws InterruptedException {
		startupLatch.await(timeout, unit);
	}

	// initialize tenant in execute operation since initialization in init operation can cause Ignite kernel deadlock (deployed service waiting on uninitialized kernel services)
	@Override
	public void execute(ServiceContext ctx) throws Exception {
		// startupLatch = ignite.countDownLatch(Tenant.SERVICE_NAME + ":startup", 1, true, true);

		if (odeConfig.getBool("ode.auto-enable").orElse(false)) {
			autoEnable();
			status(TenantStatus.ONLINE);
		}
		// IgniteMessaging msg = ignite.message();
		// msg.remoteListen(TOPIC, new TenantActionListener());
		startupLatch.countDown();

	}

	private boolean tenantCacheAvailable() {
		return ignite.cache(TENANT_CACHE_NAME) != null;
	}

	private void autoEnable() throws ModuleException {
		Set<String> resolved = new HashSet<>();
		LinkedList<Module> unresolved = new LinkedList<>();
		CoreModuleImpl coreModule = null;
		for (Module m : modules) {
			if (CoreModuleImpl.CORE_MODULE_ID.equals(getId(m))) {
				coreModule = (CoreModuleImpl) m;
			}
			unresolved.add(m);
		}

		if (!tenantCacheAvailable()) {
			enable(coreModule, (UUID) null);
			createModuleConfig(CoreModuleImpl.CORE_MODULE_ID, true);

		}

		while (!unresolved.isEmpty()) {
			boolean hasResolved = false;
			for (Iterator<Module> itr = unresolved.iterator(); itr.hasNext();) {
				Module m = itr.next();
				Set<String> dependencies = new HashSet<>();
				Collections.addAll(dependencies, getDependencies(m));
				if (!CoreModuleImpl.CORE_MODULE_ID.equals(getId(m))) {
					dependencies.add(CoreModuleImpl.CORE_MODULE_ID);
				}
				if (resolved.containsAll(dependencies)) {
					Entry<UUID, BinaryObject> moduleConfig = getOrCreateModuleConfig(getId(m));
					enable(m, moduleConfig);
					resolved.add(getId(m));
					itr.remove();
					hasResolved = true;
				}
			}

			if (!hasResolved) {
				StringBuilder msg = new StringBuilder();
				for (Module m : unresolved) {
					StringBuilder dependencies = new StringBuilder();
					for (String dependency : getDependencies(m)) {
						if (dependencies.length() > 0) {
							dependencies.append(", ");
						}
						dependencies.append(dependency);
					}
					if (msg.length() > 0) {
						msg.append(", ");
					}
					msg.append(String.format("[%s -> %s]", getId(m), dependencies));
				}
				throw new ModuleException(String.format("Unable to resolve all module dependencies %s", msg));
			}
		}
	}

	private void autoDisable() throws ModuleException {
		Set<String> resolved = new HashSet<>();
		Map<String, Set<String>> unresolved = new HashMap<>();
		Set<String> coreInverseDepdendencies = new HashSet<>();
		unresolved.put(CoreModuleImpl.CORE_MODULE_ID, coreInverseDepdendencies);
		for (Module m : modules) {
			String id = getId(m);
			if (!CoreModuleImpl.CORE_MODULE_ID.equals(id)) {
				coreInverseDepdendencies.add(id);
				unresolved.putIfAbsent(id, new HashSet<>());
				for (String dependency : getDependencies(m)) {
					Set<String> inverseDepdendencies = unresolved.putIfAbsent(dependency, new HashSet<>());
					inverseDepdendencies.add(id);
				}

			}
		}

		while (!unresolved.isEmpty()) {
			boolean hasResolved = false;
			for (Iterator<java.util.Map.Entry<String, Set<String>>> itr = unresolved.entrySet().iterator(); itr.hasNext();) {
				java.util.Map.Entry<String, Set<String>> m = itr.next();
				if (resolved.containsAll(m.getValue())) {
					Entry<UUID, BinaryObject> moduleConfig = getOrCreateModuleConfig(m.getKey());
					Module module = getModule(m.getKey());
					disable(module, moduleConfig);
					resolved.add(m.getKey());
					itr.remove();
					hasResolved = true;
				}
			}

			if (!hasResolved) {
				StringBuilder msg = new StringBuilder();
				for (java.util.Map.Entry<String, Set<String>> m : unresolved.entrySet()) {
					StringBuilder dependencies = new StringBuilder();
					for (String dependency : m.getValue()) {
						if (dependencies.length() > 0) {
							dependencies.append(", ");
						}
						dependencies.append(dependency);
					}
					if (msg.length() > 0) {
						msg.append(", ");
					}
					msg.append(String.format("[%s -> %s]", m.getKey(), dependencies));
				}
				throw new ModuleException(String.format("Unable to resolve all module dependencies %s", msg));
			}
		}
	}

	private Entry<UUID, BinaryObject> getOrCreateModuleConfig(String moduleId) {
		IgniteCache<UUID, BinaryObject> configCache = ignite.cache(TENANT_CACHE_NAME).withKeepBinary();
		SqlQuery<UUID, BinaryObject> query = new SqlQuery<>("Configuration", "path = ?");
		List<Entry<UUID, BinaryObject>> existing = configCache.query(query.setArgs(modulePath(moduleId))).getAll();
		if (!existing.isEmpty()) {
			return existing.get(0);
		}
		return createModuleConfig(moduleId, false);

	}

	private Entry<UUID, BinaryObject> createModuleConfig(String moduleId, boolean enabled) {
		IgniteCache<UUID, BinaryObject> configCache = ignite.cache(TENANT_CACHE_NAME).withKeepBinary();
		BinaryObjectBuilder moduleConfigBuilder = ignite.binary().builder("Configuration");
		moduleConfigBuilder.setField("type", "module");
		moduleConfigBuilder.setField("path", modulePath(moduleId));
		moduleConfigBuilder.setField("modifiedTime", ZonedDateTime.now(ZoneId.systemDefault()));
		moduleConfigBuilder.setField("enabled", enabled);
		BinaryObject moduleConfig = moduleConfigBuilder.build();
		UUID id = UUID.randomUUID();
		configCache.put(id, moduleConfig);
		return new CacheEntryImpl(id, moduleConfig);
	}

	private String modulePath(String moduleId) {
		return "/modules/" + moduleId;
	}

	private TenantStatus getTenantStatus(BinaryObject config) {
		if ((Boolean) config.field("online")) {
			return TenantStatus.ONLINE;
		}
		return TenantStatus.OFFLINE;
	}

	private BinaryObject getTenantStatusConfig() {
		IgniteCache<UUID, BinaryObject> configCache = ignite.cache(TENANT_CACHE_NAME).withKeepBinary();
		if (tenantConfigKey == null) {
			SqlQuery<UUID, BinaryObject> query = new SqlQuery<>("Configuration", "path = '/tenant'");
			List<Entry<UUID, BinaryObject>> existing = configCache.query(query).getAll();
			if (!existing.isEmpty()) {
				Entry<UUID, BinaryObject> result = existing.get(0);
				tenantConfigKey = result.getKey();
				return result.getValue();
			}
		} else {
			return configCache.get(tenantConfigKey);
		}
		throw new IllegalStateException("Tenant configuration not found");

	}

}
