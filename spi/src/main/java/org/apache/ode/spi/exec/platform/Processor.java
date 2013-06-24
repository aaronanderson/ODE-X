package org.apache.ode.spi.exec.platform;

public interface Processor {
	public void submit(Object operation);

	public static class ProcessorException extends Exception {

	}

	public static class InvalidOperationException extends ProcessorException {

	}
	
	public static class UnavailableException extends ProcessorException {

	}
}
