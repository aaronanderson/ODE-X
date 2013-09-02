package org.apache.ode.runtime.core.work;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

import org.apache.ode.spi.work.ExecutionUnit.BufferInput;
import org.apache.ode.spi.work.ExecutionUnit.BufferOutput;
import org.apache.ode.spi.work.ExecutionUnit.InBuffer;
import org.apache.ode.spi.work.ExecutionUnit.OutBuffer;

public class OutBufferStage extends Stage implements BufferOutput<OutBuffer> {

	protected OutBuffer bufferObject;

	public OutBufferStage(OutBuffer bufferObject) {
		super(null, new Object[bufferLength(bufferObject)]);
		this.bufferObject = bufferObject;
	}

	public static int bufferLength(Object bufferObject) {
		return bufferObject.getClass().getFields().length;
	}

	@Override
	protected void preOutput() throws StageException {
		try {
			read(bufferObject, output);
		} catch (Throwable e) {
			throw new StageException(e);
		}
	}

	public static void read(Object bufferObject, Object[] output) throws Throwable {
		if (bufferObject != null) {
			Field[] fields = bufferObject.getClass().getFields();
			for (int i = 0; i < fields.length; i++) {
				MethodHandle mh = MethodHandles.lookup().unreflectGetter(fields[i]);
				output[i] = mh.invoke(bufferObject);
			}
		}
	}

	@Override
	public OutBuffer buffer() {
		return bufferObject;
	}

}
