package org.apache.ode.tomcat;

import org.apache.catalina.startup.Tomcat;

public class TomcatConfigureEvent {

	private final Tomcat tomcat;

	public TomcatConfigureEvent(Tomcat tomcat) {
		this.tomcat = tomcat;
	}

	public Tomcat tomcat() {
		return tomcat;
	}

}
