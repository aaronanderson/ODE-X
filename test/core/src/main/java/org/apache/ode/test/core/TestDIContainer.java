package org.apache.ode.test.core;

import org.apache.ode.spi.di.DIContainer;

public interface TestDIContainer extends DIContainer{

	//maven junit concurrency: parallel=classes; beforeClass/afterClass, constructor and all tests method run on same thread
	public static final ThreadLocal<TestDIContainer> CONTAINER = new ThreadLocal<TestDIContainer>();

}
