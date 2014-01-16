package org.apache.ode.runtime.core.work;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.apache.ode.runtime.core.work.ExecutionUnitBuilder.Frame;
import org.apache.ode.spi.work.ExecutionUnit.ExecutionUnitException;
import org.apache.ode.spi.work.ExecutionUnit.In;
import org.apache.ode.spi.work.ExecutionUnit.InBuffer;
import org.apache.ode.spi.work.ExecutionUnit.InOut;
import org.apache.ode.spi.work.ExecutionUnit.Job;
import org.apache.ode.spi.work.ExecutionUnit.Out;
import org.apache.ode.spi.work.ExecutionUnit.OutBuffer;

public class InstanceExec extends ExecutionStage {
	Object instance;
	Object inputBuffer = null;
	Object outputBuffer = null;

	public InstanceExec(Frame parent, Job job) throws ExecutionUnitException {
		super(null, null, parent, Mode.JOB);
		this.instance = job;
	}

	public InstanceExec(Frame parent, In<?> in) throws ExecutionUnitException {
		this(bufferInfo(in), parent, Mode.IN);
		this.instance = in;
	}

	public InstanceExec(Frame parent, Out<?> out) throws ExecutionUnitException {
		this(bufferInfo(out), parent, Mode.OUT);
		this.instance = out;
	}

	public InstanceExec(Frame parent, InOut<?, ?> inOut) throws ExecutionUnitException {
		this(bufferInfo(inOut), parent, Mode.INOUT);
		this.instance = inOut;
	}

	public InstanceExec(BufferInfo info, Frame parent, Mode mode) throws ExecutionUnitException {
		super(info.input, info.output, parent, mode);
		this.inputBuffer = info.inputBuffer;
		this.outputBuffer = info.outputBuffer;
	}

	public static class BufferInfo {
		Object[] input = null;
		Object[] output = null;
		Object inputBuffer = null;
		Object outputBuffer = null;
	}

	public static BufferInfo bufferInfo(In<?> in) throws ExecutionUnitException {
		BufferInfo info = new BufferInfo();
		Type inputType = null;
		for (Type t : in.getClass().getGenericInterfaces()) {
			if (t instanceof ParameterizedType) {
				ParameterizedType tp = (ParameterizedType) t;
				if (tp.getRawType() == In.class) {
					inputType = tp.getActualTypeArguments()[0];
				}
			}
		}
		info.inputBuffer = newInstance("In Input", inputType);
		info.input = new Object[info.inputBuffer.getClass().getFields().length];
		return info;
	}

	public static BufferInfo bufferInfo(Out<?> out) throws ExecutionUnitException {
		BufferInfo info = new BufferInfo();
		Type outputType = null;
		for (Type t : out.getClass().getGenericInterfaces()) {
			if (t instanceof ParameterizedType) {
				ParameterizedType tp = (ParameterizedType) t;
				if (tp.getRawType() == Out.class) {
					outputType = tp.getActualTypeArguments()[0];
				}
			}
		}
		info.outputBuffer = newInstance("Out Output", outputType);
		info.output = new Object[info.outputBuffer.getClass().getFields().length];
		return info;
	}

	public static BufferInfo bufferInfo(InOut<?, ?> inOut) throws ExecutionUnitException {
		BufferInfo info = new BufferInfo();
		Type inputType = null;
		Type outputType = null;
		for (Type t : inOut.getClass().getGenericInterfaces()) {
			if (t instanceof ParameterizedType) {
				ParameterizedType tp = (ParameterizedType) t;
				if (tp.getRawType() == InOut.class) {
					inputType = tp.getActualTypeArguments()[0];
					outputType = tp.getActualTypeArguments()[1];
				}
			}
		}
		info.inputBuffer = newInstance("InOut Input", inputType);
		info.input = new Object[info.inputBuffer.getClass().getFields().length];
		info.outputBuffer = newInstance("InOut Output", outputType);
		info.output = new Object[info.outputBuffer.getClass().getFields().length];
		return info;
	}

	public static Object newInstance(String kind, Type aType) throws ExecutionUnitException {
		if (aType instanceof Class) {
			try {
				return ((Class) aType).newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new ExecutionUnitException(e);
			}
		} else {
			throw new ExecutionUnitException(String.format("Invalid %s buffer class %s", kind, aType));
		}
	}

	//If the instance has a buffer input then we want to read that into input object array in case it contains a collection object or some other aggregate holder that a transform can act on
	@Override
	protected void preInput() throws StageException {
		switch (mode) {
		case IN:
		case INOUT:
			try {
				OutBufferStage.read(inputBuffer, input);
			} catch (Throwable e) {
				throw new StageException(e);
			}
		}
	}

	@Override
	protected void postInput() throws StageException {
		switch (mode) {
		case IN:
		case INOUT:
			try {
				InBufferStage.write(input, inputBuffer);
			} catch (Throwable e) {
				throw new StageException(e);
			}
		}
	}

	@Override
	protected void preOutput() throws StageException {
		switch (mode) {
		case OUT:
		case INOUT:
			try {
				OutBufferStage.read(outputBuffer, output);
			} catch (Throwable e) {
				throw new StageException(e);
			}
		}
	}

	@Override
	public void exec() throws Throwable {

		switch (mode) {
		case JOB:
			((Job) instance).run(new WorkItemImpl(frame, this));
			break;
		case IN:
			((In) instance).in(new WorkItemImpl(frame, this), (InBuffer) inputBuffer);
			break;
		case INOUT:
			((InOut) instance).inOut(new WorkItemImpl(frame, this), (InBuffer) inputBuffer, (OutBuffer) outputBuffer);
			break;
		case OUT:
			((Out) instance).out(new WorkItemImpl(frame, this), (OutBuffer) outputBuffer);
			break;
		default:
			break;

		}

	}

}
