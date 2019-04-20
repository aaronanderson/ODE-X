package org.apache.ode.tomcat;

import static org.apache.ode.spi.config.Config.ODE_BASE_DIR;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.apache.ignite.Ignite;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.services.ServiceContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.ode.spi.CDIService;
import org.apache.ode.spi.webserver.WebServer;

public class WebServerImpl extends CDIService implements WebServer {

	public static final Logger LOG = LogManager.getLogger(WebServerImpl.class);

	@IgniteInstanceResource
	private Ignite ignite;

	@Inject
	private transient Event<TomcatConfigureEvent> configureEvent;

	private transient Tomcat tomcat;

	@Override
	public void cancel(ServiceContext ctx) {
		try {
			tomcat.stop();
		} catch (LifecycleException e) {
			LOG.error("Tomcat shutdown error", e);
		}
		super.cancel(ctx);
	}

	@Override
	public void execute(ServiceContext ctx) throws Exception {
		tomcat = new Tomcat();
		tomcat.setPort(8080);
		tomcat.getConnector();
		Path baseDir = Paths.get((String) ignite.configuration().getUserAttributes().get(ODE_BASE_DIR));
		Path tomcatBaseDir = baseDir.resolve("tomcat");
		tomcat.setBaseDir(tomcatBaseDir.toAbsolutePath().toString());
		//tomcat.getHost().setAppBase(".");
		configureEvent.fire(new TomcatConfigureEvent(tomcat));

		tomcat.start();
		tomcat.getServer().await();
	}

	@Override
	public void deploy(UUID application) {

	}

}
