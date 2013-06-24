package org.apache.ode.runtime.memory.exec.platform;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.ode.spi.exec.context.xml.ExecutionContext;


public class Execution {
	
	public static enum State {
		ACTIVE, ACTIVATING, INACTIVE, INACTIVATING; 
	}
	
	ExecutionContext executionContext;
	ReadWriteLock lock = new ReentrantReadWriteLock();
	
	

}
