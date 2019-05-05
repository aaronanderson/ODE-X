package org.apache.ode.spi.deployment;

import java.net.URI;

import org.apache.ignite.igfs.IgfsPath;

public interface CompositeManager {
	public static final String SERVICE_NAME = "urn:org:apache:ode:composite";

	public static final IgfsPath COMPOSITE_DIR = new IgfsPath("/composites");

	public <C> void create(URI reference, C config);

	public <C> void update(URI reference, C config);

	public void delete(URI reference);

	public void activate(URI reference);

	public void deactivate(URI reference);

	public void instrument(URI reference);

	public void createAlias(String alias, URI reference);

	public void deleteAlias(String alias);

	public URI alias(String alias);

}
