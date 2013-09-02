package org.apache.ode.runtime.core.work;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

import org.apache.ode.spi.work.ExecutionUnit.BufferInput;
import org.apache.ode.spi.work.ExecutionUnit.InBuffer;

public class InBufferStage extends Stage implements BufferInput<InBuffer> {

	protected InBuffer bufferObject;

	public InBufferStage(InBuffer bufferObject) {
		super(new Object[bufferLength(bufferObject)], null);
		this.bufferObject = bufferObject;
	}

	public static int bufferLength(Object bufferObject) {
		return bufferObject.getClass().getFields().length;
	}

	public static void write(Object[] input, Object bufferObject) throws Throwable {
		if (input != null && bufferObject != null) {
			Field[] fields = bufferObject.getClass().getFields();
			for (int i = 0; i < fields.length; i++) {
				MethodHandle mh = MethodHandles.lookup().unreflectSetter(fields[i]);
				mh.invoke(bufferObject, input[i]);
			}
		}
	}

	@Override
	protected void postInput() throws StageException {
		try {
			write(input, bufferObject);
		} catch (Throwable e) {
			throw new StageException(e);
		}
	}

	@Override
	public InBuffer buffer() {
		return bufferObject;
	}

}
