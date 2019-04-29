package org.apache.ode.spi.deployment;

import java.net.URI;

public interface CompositeManager {
	public static final String SERVICE_NAME = "urn:org:apache:ode:composite";

	public void create(URI reference);

	public void update(URI reference);

	public void delete(URI reference);

	public void activate(URI reference);

	public void deactivate(URI reference);

	public void instrument(URI reference);

}
