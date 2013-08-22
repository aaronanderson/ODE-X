package org.apache.ode.runtime.memory.work;

import org.apache.ode.runtime.memory.work.ExecutionUnitBuilder.Frame;
import org.apache.ode.spi.work.ExecutionUnit.In;
import org.apache.ode.spi.work.ExecutionUnit.InOut;
import org.apache.ode.spi.work.ExecutionUnit.Job;
import org.apache.ode.spi.work.ExecutionUnit.Out;

public class InstanceExec extends ExecutionStage {
	Object instance;

	public InstanceExec(Frame parent, Mode mode, Job job) {
		super(parent, mode);
		this.instance = job;
	}

	public InstanceExec(Frame parent, Mode mode, In<?> in) {
		super(parent, mode);
		this.instance = in;
	}

	public InstanceExec(Frame parent, Mode mode, Out<?> out) {
		super(parent, mode);
		this.instance = out;
	}

	public InstanceExec(Frame parent, Mode mode, InOut<?, ?> inOut) {
		super(parent, mode);
		this.instance = inOut;
	}

	@Override
	public void exec() throws Throwable {

	
		switch (mode) {
		case JOB:
			((Job) instance).run(new WorkItemImpl(frame));
			break;
		case IN:
			break;
		case INOUT:
			break;
		case OUT:
			break;
		default:
			break;

		}
		//TODO check outpipes

	}

}
