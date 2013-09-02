package org.apache.ode.runtime.core.work;

import org.apache.ode.spi.work.ExecutionUnit.ArrayOutput;

public class OutArrayStage extends Stage implements ArrayOutput {

	protected Object[] parameters;

	public OutArrayStage(Object[] parameters) {
		super(null, new Object[parameters.length]);
		this.parameters = parameters;
	}

	@Override
	protected void preOutput() throws StageException {
		try {
			InArrayStage.write(parameters, output);
		} catch (Throwable e) {
			throw new StageException(e);
		}
	}
	

	@Override
	public Object[] array() {
		return parameters;
	}

}
