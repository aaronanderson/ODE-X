package org.apache.ode.spi.deployment;

import java.net.URI;

public interface AssemblyManager {
	public static final String SERVICE_NAME = "urn:org:apache:ode:assembly";
	
	public void create(URI reference);
	
	public void update(URI reference);
	
	public void delete(URI reference);
	
	public void deploy(URI reference);
	
	public void undeploy(URI reference);

}
