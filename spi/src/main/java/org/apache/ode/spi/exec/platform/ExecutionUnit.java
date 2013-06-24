package org.apache.ode.spi.exec.platform;

import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

public interface ExecutionUnit {

	//Fluent API

	//current ExecutionUnit
	public void blocked(QName pollOperationName);

	public void abort(QName handlerOperationName, Object... input);

	public void cancel();
	
	//New ExecutionUnits

	public ExecutionUnit setEnvironment(Map<QName, ?> values);

	public ExecutionUnit unsetEnvironment(Set<QName> keys);

	public ExecutionUnit runCommand(QName commandName, Object... input);

	public ExecutionUnit runOperation(QName operationName, Object... input);

	public ExecutionUnit pipeTo(ExecutionUnit execUnit);

	public ExecutionUnit beginSequential();

	public ExecutionUnit endSequential();

	public ExecutionUnit beginParallel();

	public ExecutionUnit endParallel();

}
