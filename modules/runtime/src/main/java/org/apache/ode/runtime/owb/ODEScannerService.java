package org.apache.ode.runtime.owb;

import java.io.File;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;
import java.util.jar.JarFile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.ode.runtime.owb.ODEOWBInitializer.ContainerMode;
import org.apache.openwebbeans.se.CDISeScannerService;
import org.apache.xbean.finder.archive.FileArchive;
import org.apache.xbean.finder.util.Files;

public class ODEScannerService extends CDISeScannerService {

	public static final Logger LOG = LogManager.getLogger(ODEScannerService.class);
	public static final String ODE_MODULE_PATH = "META-INF/ode-module.yml";
	private ContainerMode containerMode = ContainerMode.SERVER;

	public ContainerMode getContainerMode() {
		return containerMode;
	}

	public void setContainerMode(ContainerMode containerMode) {
		this.containerMode = containerMode;
	}

	@Override
	protected void filterExcludedJars(Set<URL> classPathUrls) {
		super.filterExcludedJars(classPathUrls);
		if (containerMode == ContainerMode.SERVER) {
			Iterator<URL> it = classPathUrls.iterator();
			while (it.hasNext()) {
				URL url = it.next();
				try {
					String jarPath = url.getFile();
					if (jarPath.contains("!")) {
						jarPath = jarPath.substring(0, jarPath.indexOf("!"));
						url = new URL(jarPath);
					}
					if (!"file".equals(url.getProtocol())) {
						LOG.error("unsupported protocol {}, skipping.", url.getProtocol());
						continue;
					}
					File file = Files.toFile(url);
					if (file.exists() && file.isDirectory()) {
						File odeModuleFile = new File(file, ODE_MODULE_PATH);
						if (!odeModuleFile.exists()) {
							LOG.debug("{} does not contain ode-module.yml, skipping", url);
							it.remove();
						}
					} else if (file.exists() && file.isFile()) {
						try (JarFile jar = new JarFile(FileArchive.decode(url.getFile()));) {
							if (jar.getEntry(ODE_MODULE_PATH) == null) {
								LOG.debug("{} does not contain ode-module.yml, skipping", url);
								it.remove();
							}
						}
					}
				} catch (Exception e) {
					LOG.error("Unable to inspect URL", e);
					it.remove();
				}
				// it.remove();
			}

		}
	}

}
