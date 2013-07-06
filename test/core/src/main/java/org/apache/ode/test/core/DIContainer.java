package org.apache.ode.test.core;

public interface DIContainer {

	<T> T getInstance(Class<T> clazz);

	//maven junit concurrency: parallel=classes; beforeClass/afterClass, constructor and all tests method run on same thread
	public static final ThreadLocal<DIContainer> CONTAINER = new ThreadLocal<DIContainer>();

}
