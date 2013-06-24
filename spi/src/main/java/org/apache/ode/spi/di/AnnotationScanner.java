package org.apache.ode.spi.di;

import java.util.Map;

public interface AnnotationScanner<M> {
	//this is used the system's dependency injection system to scan classes exposed for dependency injection and then 
	//build a meta-model on how those classes will be acted upon in ODE. After scanning is complete The instance should then be made
	//available for injection through the DI container

	public M scan(Class<?> clazz);


	

}
