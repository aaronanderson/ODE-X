package org.apache.ode.runtime.interpreter;

import java.util.concurrent.Callable;

public class Cycle implements Callable<Cycle>{

	@Override
	public Cycle call() throws Exception {
		return this;
	}
	

}
