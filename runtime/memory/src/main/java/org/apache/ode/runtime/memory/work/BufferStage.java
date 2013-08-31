package org.apache.ode.runtime.memory.work;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

import org.apache.ode.spi.work.ExecutionUnit.InBuffer;
import org.apache.ode.spi.work.ExecutionUnit.OutBuffer;

public class BufferStage extends Stage {

	protected Object bufferObject;
	protected final boolean inMode;

	public BufferStage(InBuffer bufferObject) {
		super(new Object[bufferLength(bufferObject)], null);
		this.bufferObject = bufferObject;
		inMode = true;
	}

	public BufferStage(OutBuffer bufferObject) {
		super(null, new Object[bufferLength(bufferObject)]);
		this.bufferObject = bufferObject;
		inMode = false;
	}

	public static int bufferLength(Object bufferObject) {
		return bufferObject.getClass().getFields().length;
	}
	
	@Override
	protected void preOutput() throws StageException {
		if (!inMode) {
			try {
				read(bufferObject, output);
			} catch (Throwable e) {
				throw new StageException(e);
			}
		}
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
		if (inMode) {
			try {
				write(input, bufferObject);
			} catch (Throwable e) {
				throw new StageException(e);
			}
		}
	}

	public static Object[] read(Object bufferObject, Object[] output) throws Throwable {
		if (bufferObject != null) {
			Field[] fields = bufferObject.getClass().getFields();
			for (int i = 0; i < fields.length; i++) {
				MethodHandle mh = MethodHandles.lookup().unreflectGetter(fields[i]);
				output[i] = mh.invoke(bufferObject);
			}
			return output;
		}
		return null;
	}

}
