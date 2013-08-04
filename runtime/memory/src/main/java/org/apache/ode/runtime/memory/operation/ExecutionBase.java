package org.apache.ode.runtime.memory.operation;

import java.util.concurrent.TimeUnit;

import javax.xml.namespace.QName;

import org.apache.ode.spi.work.ExecutionUnit;
import org.apache.ode.spi.work.ExecutionUnit.ExecutionState;
import org.apache.ode.spi.work.ExecutionUnit.ExecutionUnitException;
import org.apache.ode.spi.work.ExecutionUnit.InExecution;
import org.apache.ode.spi.work.ExecutionUnit.InOutExecution;
import org.apache.ode.spi.work.ExecutionUnit.InStream;
import org.apache.ode.spi.work.ExecutionUnit.OutExecution;
import org.apache.ode.spi.work.ExecutionUnit.OutStream;

public class ExecutionBase implements InExecution, OutExecution, InOutExecution {

	Mode mode;

	public ExecutionBase(Mode mode) {
		this.mode = mode;
	}

	public static enum Mode {
		IN, OUT, INOUT;
	}

	@Override
	public ExecutionState state(long timeout, TimeUnit unit, ExecutionState... expected) throws ExecutionUnitException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <I extends InStream> OutExecution pipeOut(I stream) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InExecution pipeOut(InExecution execUnit, int... map) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InOutExecution pipeOut(InOutExecution execUnit, int... map) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <O extends OutStream> InExecution pipeIn(O stream) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OutExecution pipeIn(OutExecution execUnit, int... map) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InOutExecution pipeIn(InOutExecution execUnit, int... map) {
		// TODO Auto-generated method stub
		return null;
	}

	

}
