package org.apache.ode.spi.webserver;

import java.util.UUID;

public interface WebServer {
	
	public static final String SERVICE_NAME = "urn:org:apache:ode:webserver";

	public void deploy(UUID application);

}
