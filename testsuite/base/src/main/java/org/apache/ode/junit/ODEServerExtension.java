package org.apache.ode.junit;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import org.apache.ode.runtime.Server;
import org.apache.ode.spi.config.Config;
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
		server = Server.instance();
		server.containerInitializer().addBeanClasses(context.getRequiredTestClass());
		server.containerInitializer().addBeanClasses(context.getRequiredTestClass().getDeclaredClasses());
		String configFile = "ode-test.yml";
		OdeServer config = context.getRequiredTestClass().getAnnotation(OdeServer.class);
		if (config != null) {
			configFile = config.config();
		}
		server.start(configFile);
		server.clusterAutoOnline();
	}

	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		String odeHome = (String) server.ignite().configuration().getUserAttributes().get(Config.ODE_HOME);
		server.close();
		Files.walk(Paths.get(odeHome)).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);

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
