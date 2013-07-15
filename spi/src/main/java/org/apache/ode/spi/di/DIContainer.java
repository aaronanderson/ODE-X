package org.apache.ode.spi.di;

import java.lang.annotation.Annotation;

public interface DIContainer {

	<T> T getInstance(Class<T> clazz);
	
	<T> T getInstance(Class<T> clazz, Annotation qualifier);

}
