package org.apache.ode.spi.deployment;

import java.net.URI;
import java.nio.file.Path;

public interface DevelopmentManager {
	public static final String SERVICE_NAME = "urn:org:apache:ode:development";

	public void register(URI reference, Path assemblyPath);

	public void unregister(URI reference, Path assemblyPath);


}
