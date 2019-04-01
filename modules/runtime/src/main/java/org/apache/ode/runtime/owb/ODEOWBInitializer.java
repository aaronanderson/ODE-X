package org.apache.ode.runtime.owb;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;

import org.apache.openwebbeans.se.OWBContainer;
import org.apache.openwebbeans.se.OWBInitializer;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.corespi.DefaultSingletonService;
import org.apache.webbeans.spi.ContainerLifecycle;
import org.apache.webbeans.spi.ScannerService;
import org.apache.webbeans.spi.SingletonService;

public class ODEOWBInitializer extends OWBInitializer {

	public static final String CONTAINER_MODE = "ode.container.mode";

	public static enum ContainerMode {
		SERVER, ASSEMBLY;
	}

	private ContainerMode mode = ContainerMode.SERVER;
	protected ODEScannerService odeScannerService = new ODEScannerService();

	public ODEOWBInitializer() {
		odeScannerService.loader(loader);
	}

	@Override
	public SeContainerInitializer addProperty(String key, Object value) {
		if (CONTAINER_MODE.equals(key) && ContainerMode.class.isInstance(value)) {
			mode = (ContainerMode) value;
			odeScannerService.setContainerMode(mode);

			return this;
		}
		return super.addProperty(key, value);
	}

	protected ScannerService getScannerService() {
		return odeScannerService;
	}

	@Override
	public SeContainerInitializer addBeanClasses(Class<?>... classes) {
		odeScannerService.classes(classes);
		return this;
	}

	@Override
	public SeContainerInitializer addPackages(boolean scanRecursively, Package... packages) {
		odeScannerService.packages(scanRecursively, packages);
		return this;
	}

	@Override
	public SeContainerInitializer addPackages(boolean scanRecursively, Class<?>... packageClasses) {
		odeScannerService.packages(scanRecursively, packageClasses);
		return this;
	}

	@Override
	public SeContainerInitializer disableDiscovery() {
		odeScannerService.disableAutoScanning();
		return this;
	}

	@Override
	public SeContainerInitializer setClassLoader(ClassLoader classLoader) {
		loader = classLoader;
		odeScannerService.loader(loader);
		return this;
	}

	@Override
	protected SeContainer newContainer(final WebBeansContext context) {
		Object startObj = new Object();
		context.getService(ContainerLifecycle.class).startApplication(startObj);
		return new OWBContainer(context, startObj);
	}

	public class ODEOWBContainer extends OWBContainer {

		public ODEOWBContainer(WebBeansContext context, Object startObj) {
			super(context, startObj);
		}

		@Override
		protected void doClose() {
			super.doClose();
			SingletonService<WebBeansContext> singletonInstance = WebBeansFinder.getSingletonService();
			DefaultSingletonService.class.cast(singletonInstance).clear(loader);
		}

	}
}
