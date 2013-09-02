package org.apache.ode.runtime.core.work;

import java.lang.reflect.Array;

import org.apache.ode.spi.work.ExecutionUnit.ArrayInput;

public class InArrayStage extends Stage implements ArrayInput {

	protected Object[] parameters;

	public InArrayStage(Object[] parameters) {
		super(new Object[parameters.length], null);
		this.parameters = parameters;
	}

	public static void write(Object[] src, Object[] dst) throws Throwable {
		if (src != null && dst != null) {
			//deep copy
			for (int i = 0; i < src.length; i++) {
				if (src[i] != null && dst[i] != null && src[i].getClass().isArray() /*&& dst[i].getClass().isArray()*/) {
					System.arraycopy(src[i], 0, dst[i], 0,  Array.getLength(src[i]));
				} else {
					dst[i] = src[i];
				}
			}
			//System.arraycopy(src, 0, dst, 0, src.length);

		}
	}

	@Override
	protected void postInput() throws StageException {
		try {
			write(input, parameters);
		} catch (Throwable e) {
			throw new StageException(e);
		}
	}

	@Override
	public Object[] array() {
		return parameters;
	}

}
