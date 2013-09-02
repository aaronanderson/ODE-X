package org.apache.ode.runtime.core.work;

import java.lang.reflect.Array;

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
			read(parameters, output);
		} catch (Throwable e) {
			throw new StageException(e);
		}
	}

	/*public static void read(Object[] src, Object[] dst) throws Throwable {
		if (src != null) {
			System.arraycopy(src, 0, dst, 0, src.length);
		}
	}*/
	
	public static void read(Object[] src, Object[] dst) throws Throwable {
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
	public Object[] array() {
		return parameters;
	}

}
