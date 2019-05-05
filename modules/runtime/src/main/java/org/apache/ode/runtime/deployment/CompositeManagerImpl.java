package org.apache.ode.runtime.deployment;

import java.net.URI;

import org.apache.ode.spi.CDIService;
import org.apache.ode.spi.deployment.CompositeManager;

public class CompositeManagerImpl extends CDIService implements CompositeManager {

	@Override
	public <C> void create(URI reference, C config) {
		// TODO Auto-generated method stub

	}

	@Override
	public <C> void update(URI reference, C config) {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(URI reference) {
		// TODO Auto-generated method stub

	}

	@Override
	public void activate(URI reference) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deactivate(URI reference) {
		// TODO Auto-generated method stub

	}

	@Override
	public void instrument(URI reference) {
		// TODO Auto-generated method stub

	}

	@Override
	public void createAlias(String alias, URI reference) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteAlias(String alias) {
		// TODO Auto-generated method stub

	}

	@Override
	public URI alias(String alias) {
		// TODO Auto-generated method stub
		return null;
	}

}
