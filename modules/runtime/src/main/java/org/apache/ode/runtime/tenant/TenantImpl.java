package org.apache.ode.runtime.tenant;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCountDownLatch;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.services.ServiceContext;
import org.apache.ode.spi.CDIService;
import org.apache.ode.spi.config.Config;
import org.apache.ode.spi.tenant.Module;
import org.apache.ode.spi.tenant.Module.ModuleStatus;
import org.apache.ode.spi.tenant.Tenant;

@ApplicationScoped
public class TenantImpl extends CDIService implements Tenant {
	public static final String TOPIC = "ODETenantTopic";

	@Inject
	@Any
	private Instance<Module> modules;

	// private Map<String, Object> moduleMap = new HashMap<>();

	@Inject
	Config odeConfig;

	@IgniteInstanceResource
	private Ignite ignite;

	private TenantStatus tenantStatus = TenantStatus.OFFLINE;

	private IgniteCountDownLatch startupLatch;

	@PostConstruct
	public void init() {
//		for (Module module : modules) {
//			moduleMap.put(module.id(), module);
//		}
	}

	@Override
	public Set<String> modules() {
		return modules.stream().map(m -> m.id()).collect(Collectors.toSet());
	}

	@Override
	public <M> M instance(String moduleId) throws ModuleException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ModuleStatus status(String moduleId) throws ModuleException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void enable(String moduleId) throws ModuleException {
		// IgniteMessaging msg = ignite.message();
		// msg.sendOrdered(TOPIC, new EnableModule().moduleId(moduleId), 0);
	}

	@Override
	public void disable(String moduleId) throws ModuleException {
		// IgniteMessaging msg = ignite.message();
		// msg.sendOrdered(TOPIC, new DisableModule().moduleId(moduleId), 0);
	}

	@Override
	public void init(ServiceContext ctx) throws Exception {
		super.init(ctx);
		// initialize startup countdown latch
		startupLatch = ignite.countDownLatch(Tenant.SERVICE_NAME + ":startup", 1, false, true);

		Set<Module> resolved = new HashSet<>();
		// Core module
		CoreModuleImpl coreModule = (CoreModuleImpl) modules.stream().filter(m -> CoreModuleImpl.CORE_MODULE_ID.equals(m.id())).findFirst().orElseThrow(() -> new ModuleException("Core module not found"));

		if (odeConfig.getBool("ode.auto-enable").orElse(false)) {
			// Tenant service is automatically enabled on all nodes
			CacheConfiguration tenantCacheCfg = new CacheConfiguration("Tenant");
			tenantCacheCfg.setCacheMode(CacheMode.REPLICATED);

			// CacheConfiguration processCacheCfg = new CacheConfiguration("Process");
			// processCacheCfg.setCacheMode(CacheMode.PARTITIONED);

			// igniteConfig.setCacheConfiguration(processCacheCfg, tenantCacheCfg);

		}
		// IgniteMessaging msg = ignite.message();
		// msg.remoteListen(TOPIC, new TenantActionListener());
		startupLatch.countDown();
		tenantStatus = TenantStatus.ONLINE;
	}

	@Override
	public TenantStatus status() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void awaitStart(long timeout, TimeUnit unit) {
		startupLatch.await(timeout, unit);
	}

//	private void handle(TenantAction action) {
//		throw new UnsupportedOperationException("Unsupported action " + action);
//	}
//
//	private void handle(EnableModule action) {
//		if (ALL_MODULES.equals(action.moduleId())) {
//			
//		}
//
//	}
//
//	private void handle(DisableModule action) {
//
//	}
//
//	public static interface TenantAction extends Serializable {
//
//	}
//
//	public static class EnableModule implements TenantAction {
//
//		private String moduleId;
//
//		public String moduleId() {
//			return moduleId;
//		}
//
//		public EnableModule moduleId(String moduleId) {
//			this.moduleId = moduleId;
//			return this;
//		}
//
//	}
//
//	public static class DisableModule implements TenantAction {
//
//		private String moduleId;
//
//		public String moduleId() {
//			return moduleId;
//		}
//
//		public DisableModule moduleId(String moduleId) {
//			this.moduleId = moduleId;
//			return this;
//		}
//
//	}
//
//	public class TenantActionListener implements IgniteBiPredicate<UUID, TenantAction> {
//
//		@Override
//		public boolean apply(UUID nodeId, TenantAction action) {
//			System.out.println("Received ordered message [msg=" + action + ", from=" + nodeId + ']');
//			handle(action);
//			return true; // Return true to continue listening.
//		}
//
//	}

}
