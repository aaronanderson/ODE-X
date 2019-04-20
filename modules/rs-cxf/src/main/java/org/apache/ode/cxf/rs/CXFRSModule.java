package org.apache.ode.cxf.rs;

import org.apache.ode.spi.tenant.Module;
import org.apache.ode.spi.tenant.Module.Id;
import org.apache.ode.tomcat.TomcatModule;

@Id(value = CXFRSModule.CSFRS_MODULE_ID, dependencies = { TomcatModule.TOMCAT_MODULE_ID })
public class CXFRSModule implements Module {

	public static final String CSFRS_MODULE_ID = "org:apache:ode:cxf:rs";

	// no need to do anything, JaxrsServletContainerInitializer is in the classpath and will get loaded by the ServiceLoader API

}
