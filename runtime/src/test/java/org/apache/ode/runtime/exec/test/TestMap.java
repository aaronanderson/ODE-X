package org.apache.ode.runtime.exec.test;

import org.apache.ode.runtime.ectx.test.xml.TestVariables.TestVariable;
import org.apache.ode.spi.exec.ListMap;

public class TestMap<T extends TestVariable> extends ListMap<String, T> {

	@Override
	public String getKey(T value) {
		return value.getName();
	}

}
