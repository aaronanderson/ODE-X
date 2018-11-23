package org.apache.ode.runtime.owb;

import javax.enterprise.inject.se.SeContainerInitializer;

import org.apache.openwebbeans.se.SeContainerSelector;

public class ODEOWBSeContainerSelector implements SeContainerSelector {

	@Override
	public SeContainerInitializer find() {
		return new ODEOWBInitializer();
	}

}
