package org.apache.ode.runtime.core.work;

import java.lang.reflect.Array;
import java.lang.reflect.Type;

import org.apache.ode.runtime.core.work.ExecutionUnitBuilder.Frame;
import org.apache.ode.spi.di.OperationAnnotationScanner.BufferInput;
import org.apache.ode.spi.di.OperationAnnotationScanner.BufferOutput;
import org.apache.ode.spi.di.OperationAnnotationScanner.Input;
import org.apache.ode.spi.di.OperationAnnotationScanner.InputOutput;
import org.apache.ode.spi.di.OperationAnnotationScanner.OperationModel;
import org.apache.ode.spi.di.OperationAnnotationScanner.Output;
import org.apache.ode.spi.di.OperationAnnotationScanner.ParameterInfo;
import org.apache.ode.spi.work.ExecutionUnit.InBuffer;
import org.apache.ode.spi.work.ExecutionUnit.OutBuffer;

public class OperationExec extends ExecutionStage {
	OperationModel model;
	InBuffer inputBuffer;
	OutBuffer outputBuffer;

	public OperationExec(Frame parent, OperationModel model) {
		super(model.inputCount() > 0 ? new Object[model.inputCount()] : null, model.outputCount() > 0 ? new Object[model.outputCount()] : model.hasReturn() ? new Object[1] : null,
				parent, mode(model));
		this.model = model;
	}

	public static Mode mode(OperationModel model) {
		if (model.inputCount() > 0) {
			if (model.outputCount() > 0) {
				return Mode.INOUT;
			} else {
				return Mode.IN;
			}
		} else if (model.outputCount() > 0) {
			return Mode.OUT;
		} else {
			return Mode.JOB;
		}
	}

	public static Object newInstance(String kind, Type aType) throws StageException {
		if (aType instanceof Class) {
			try {
				return ((Class) aType).newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new StageException(e);
			}
		} else {
			throw new StageException(String.format("Invalid %s buffer class %s", kind, aType));
		}
	}

	//If the instance has a buffer input then we want to read that into input object array in case it contains a collection object or some other aggregate holder that a transform can act on
	@Override
	protected void preInput() throws StageException {
		if (model.inBuffer() != null) {
			inputBuffer = (InBuffer) newInstance("InBuffer", model.inBuffer());
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
		if (model.outBuffer() != null) {
			outputBuffer = (OutBuffer) newInstance("OutBuffer", model.outBuffer());
		}
	}

	@Override
	protected void postInput() throws StageException {
		if (inputBuffer != null) {
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
	}

	@Override
	protected void preOutput() throws StageException {
		if (outputBuffer != null) {
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
	}

	//Since Java does not have pointers use Arrays of size one as substitute
	@Override
	public void exec() throws Throwable {
		ParameterInfo[] pis = model.parameterInfo();
		Object[] invokeParams = new Object[pis.length];
		int outputIndex = 0;

		for (int i = 0; i < pis.length; i++) {
			if (pis[i] instanceof Input) {
				Input pInput = (Input) pis[i];
				if (pInput.inject) {
					if (pInput.qualifier != null) {
						invokeParams[i] = frame.workCtx.dic.getInstance(pInput.paramType, pInput.qualifier);
					} else {
						invokeParams[i] = frame.workCtx.dic.getInstance(pInput.paramType);
					}
				} else {
					invokeParams[i] = input[pInput.index];
				}
				//Array Coerce if necessary
				if (pInput.paramType.isArray() && invokeParams[i] != null && !invokeParams[i].getClass().isArray()) {
					Object newArray = Array.newInstance(pInput.paramType.getComponentType(), 1);
					Array.set(newArray, 0, invokeParams[i]);
					invokeParams[i] = newArray;
				}

			} else if (pis[i] instanceof Output) {
				Output pOutput = (Output) pis[i];
				if (pOutput.inject) {
					if (pOutput.qualifier != null) {
						invokeParams[i] = frame.workCtx.dic.getInstance(pOutput.paramType, pOutput.qualifier);
					} else {
						invokeParams[i] = frame.workCtx.dic.getInstance(pOutput.paramType);
					}
				} else {
					//if it is an array create a holder size one
					if (pOutput.paramType.isArray()) {
						invokeParams[i] = Array.newInstance(pOutput.paramType.getComponentType(), 1);
					} else { //otherwise try to create an instance
						invokeParams[i] = pOutput.paramType.newInstance();
					}
				}
				//Array Coerce if necessary
				if (pOutput.paramType.isArray() && invokeParams[i] != null && !invokeParams[i].getClass().isArray()) {
					Object newArray = Array.newInstance(pOutput.paramType.getComponentType(), 1);
					Array.set(newArray, 0, invokeParams[i]);
					invokeParams[i] = newArray;
				}
				output[outputIndex++] = invokeParams[i];
			} else if (pis[i] instanceof InputOutput) {
				InputOutput pInput = (InputOutput) pis[i];
				if (pInput.inject) {
					if (pInput.qualifier != null) {
						invokeParams[i] = frame.workCtx.dic.getInstance(pInput.paramType, pInput.qualifier);
					} else {
						invokeParams[i] = frame.workCtx.dic.getInstance(pInput.paramType);
					}
				} else {
					invokeParams[i] = input[pInput.index];
				}
				//Array Coerce if necessary
				if (pInput.paramType.isArray() && invokeParams[i] != null && !invokeParams[i].getClass().isArray()) {
					Object newArray = Array.newInstance(pInput.paramType.getComponentType(), 1);
					Array.set(newArray, 0, invokeParams[i]);
					invokeParams[i] = newArray;
				}
				output[outputIndex++] = invokeParams[i];
			} else if (pis[i] instanceof BufferInput) {
				invokeParams[i] = inputBuffer;
			} else if (pis[i] instanceof BufferOutput) {
				invokeParams[i] = outputBuffer;

			}
		}
		Object returnVal = model.handle().invokeWithArguments(invokeParams);
		if (model.hasReturn()) {
			output[0] = returnVal;
		}
	}

}
