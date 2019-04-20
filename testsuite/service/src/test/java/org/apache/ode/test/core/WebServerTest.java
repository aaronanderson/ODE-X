package org.apache.ode.test.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.TimeUnit;

import javax.enterprise.inject.Any;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.ode.cxf.rs.CXFRSModule;
import org.apache.ode.junit.OdeServer;
import org.apache.ode.runtime.Server;
import org.apache.ode.spi.webserver.WebServer;
import org.apache.ode.tomcat.TomcatModule;
import org.junit.jupiter.api.Test;

@OdeServer
public class WebServerTest {
	Server server = null;

	public WebServerTest(Server server) {
		this.server = server;
	}

	@Test
	public void install() throws Exception {
		assertNotNull(server.container().select(TomcatModule.class, Any.Literal.INSTANCE).get());
		assertNotNull(server.container().select(CXFRSModule.class, Any.Literal.INSTANCE).get());
		WebServer webServer = server.ignite().services().serviceProxy(WebServer.SERVICE_NAME, WebServer.class, false);
		assertEquals(1, 1);
		assertNotNull(webServer);

		Client client = ClientBuilder.newBuilder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).build();
		WebTarget target = client.target("http://localhost:8080");
		try (Response response = target.request().get();) {
			assertEquals(500, response.getStatus());

		}
		client.close();
	}

}
