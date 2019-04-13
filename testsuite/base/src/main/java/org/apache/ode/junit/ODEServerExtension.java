package org.apache.ode.junit;

import org.apache.ode.runtime.Server;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class ODEServerExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {
	Server server = null;

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		server = Server.instance().start("ode-test.yml");

		// context.getTestInstanceLifecycle()

		// TcpDiscoverySpi serverDiscoverySPI = (TcpDiscoverySpi) server.configuration().getDiscoverySpi();
		// System.out.format("Server discovery address %s %d\n", serverDiscoverySPI.getLocalAddress(), serverDiscoverySPI.getLocalPort());
	}

	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		server.close();
	}

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
		return parameterContext.getParameter().getType().equals(Server.class);
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
		if (parameterContext.getParameter().getType().equals(Server.class)) {
			return server;
		}
		return null;
	}

}
