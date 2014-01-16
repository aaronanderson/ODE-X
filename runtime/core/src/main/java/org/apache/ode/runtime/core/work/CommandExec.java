package org.apache.ode.runtime.core.work;

import javax.xml.namespace.QName;

import org.apache.ode.runtime.core.work.ExecutionUnitBuilder.Frame;
import org.apache.ode.spi.di.CommandAnnotationScanner.CommandModel;
import org.apache.ode.spi.di.OperationAnnotationScanner.BaseModel;
import org.apache.ode.spi.di.OperationAnnotationScanner.OperationModel;
import org.apache.ode.spi.work.ExecutionUnit.ExecutionUnitException;

public class CommandExec extends OperationExec {

	public CommandExec(Frame parent, BaseModel model) {
		super(parent, model);
	}

	@Override
	public void exec() throws Throwable {
		//swap out abstract command model for concrete operation
		CommandModel cm = (CommandModel) model;
		OperationModel newModel = frame.workCtx.registry.resolveExecCommand(cm, input);		
		this.model = newModel;
		super.exec();
	}

}
