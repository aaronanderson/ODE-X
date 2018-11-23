package org.apache.ode.junit;

import org.apache.ode.runtime.Client;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class ODEClientExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {
	Client client = null;

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		client = Client.instance().start("ode-test.yml");
		// TcpDiscoverySpi clientDiscoverySPI = (TcpDiscoverySpi) client.configuration().getDiscoverySpi();
		// System.out.format("Client discovery address %s %d\n", clientDiscoverySPI.getLocalAddress(), clientDiscoverySPI.getLocalPort());

	}

	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		client.close();

	}

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
		return parameterContext.getParameter().getType().equals(Client.class);
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
		if (parameterContext.getParameter().getType().equals(Client.class)) {
			return client;
		}
		return null;
	}

}
