package org.apache.ode.runtime.memory.work;

import javax.xml.namespace.QName;

import org.apache.ode.runtime.memory.work.ExecutionUnitBase.ExecutionType;
import org.apache.ode.spi.work.ExecutionUnit.In;
import org.apache.ode.spi.work.ExecutionUnit.InOut;
import org.apache.ode.spi.work.ExecutionUnit.Job;
import org.apache.ode.spi.work.ExecutionUnit.Out;

public class EnvironmentAction<V> implements ExecutionType {
	final EnvMode mode;
	final QName name;
	final V value;

	EnvironmentAction(EnvMode mode, QName name, V value) {
		this.mode = mode;
		this.name = name;
		this.value = value;
	}

	public static enum EnvMode {
		SET, UNSET;
	}

}
