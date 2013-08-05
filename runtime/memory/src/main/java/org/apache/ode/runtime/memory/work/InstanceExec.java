package org.apache.ode.runtime.memory.work;

import org.apache.ode.runtime.memory.work.ExecutionUnitBase.ExecutionType;
import org.apache.ode.spi.work.ExecutionUnit.In;
import org.apache.ode.spi.work.ExecutionUnit.InOut;
import org.apache.ode.spi.work.ExecutionUnit.Job;
import org.apache.ode.spi.work.ExecutionUnit.Out;

public class InstanceExec extends ExecutionBase implements ExecutionType {
	Object instance;

	public InstanceExec(ExecutionUnitBase parent, Mode mode, Job job) {
		super(parent, mode);
		this.instance = job;
	}
	
	public InstanceExec(ExecutionUnitBase parent, Mode mode, In<?> in) {
		super(parent, mode);
		this.instance = in;
	}
	
	public InstanceExec(ExecutionUnitBase parent, Mode mode, Out<?> out) {
		super(parent, mode);
		this.instance = out;
	}
	
	public InstanceExec(ExecutionUnitBase parent, Mode mode, InOut<?,?> inOut) {
		super(parent, mode);
		this.instance = inOut;
	}

	@Override
	public void run() {
		switch(mode){
		case JOB:
			((Job)instance).run(workItem);
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

	}

}
