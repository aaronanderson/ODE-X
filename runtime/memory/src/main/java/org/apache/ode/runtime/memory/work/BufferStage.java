package org.apache.ode.runtime.memory.work;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

import org.apache.ode.spi.work.ExecutionUnit.Buffer;

public class BufferStage extends Stage {

	protected Buffer bufferObject;
	protected boolean read =false;
	protected boolean write =false;

	public BufferStage(Buffer bufferObject) {
		this.bufferObject = bufferObject;
	}

	public void read() throws StageException {
		try {
			if (bufferObject != null) {
				Field[] fields = bufferObject.getClass().getFields();
				value = new Object[fields.length];
				for (int i = 0; i < fields.length; i++) {
					MethodHandle mh = MethodHandles.lookup().unreflectGetter(fields[i]);
					value[i] = mh.invoke(bufferObject);
				}
			}
		} catch (Throwable e) {
			value = null;
			throw new StageException(e);
		}finally{
			read = true;
		}
	}

	public void write() throws StageException {
		try {
			if (bufferObject != null) {
				Field[] fields = bufferObject.getClass().getFields();
				for (int i = 0; i < fields.length; i++) {
					MethodHandle mh = MethodHandles.lookup().unreflectSetter(fields[i]);
					mh.invoke(bufferObject, value[i]);
				}
			}
		} catch (Throwable e) {
			value = null;
			throw new StageException(e);
		}finally{
			write = true;
		}
	}

}
