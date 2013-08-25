package org.apache.ode.runtime.memory.work;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

import org.apache.ode.spi.work.ExecutionUnit.InBuffer;
import org.apache.ode.spi.work.ExecutionUnit.OutBuffer;

public class BufferStage extends Stage {

	protected Object bufferObject;
	protected boolean read = false;
	protected boolean write = false;

	public BufferStage(InBuffer bufferObject) {
		super(new Object[bufferLength(bufferObject)], null);
		this.bufferObject = bufferObject;
	}

	public BufferStage(OutBuffer bufferObject) {
		super(null, new Object[bufferLength(bufferObject)]);
		this.bufferObject = bufferObject;
	}

	public static int bufferLength(Object bufferObject) {
		return bufferObject.getClass().getFields().length;
	}

	public void read() throws StageException {
		try {
			read(bufferObject, output);
		} catch (Throwable e) {
			throw new StageException(e);
		} finally {
			read = true;
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

	public void write() throws StageException {
		try {
			write(input, bufferObject);

		} catch (Throwable e) {
			throw new StageException(e);
		} finally {
			write = true;
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

}
