package org.apache.ode.runtime.exec.platform.task;

import java.util.concurrent.Callable;

import javax.persistence.EntityManager;

import org.apache.ode.runtime.exec.platform.task.TaskActionImpl.TaskActionIdImpl;
import org.apache.ode.spi.exec.PlatformException;
import org.apache.ode.spi.exec.Task.TaskActionState;
import org.w3c.dom.Document;

public class TaskActionCallable implements Callable<TaskActionState> {

	private TaskActionIdImpl id;
	private Document actionInput;
	EntityManager pmgr;
	TaskActionImpl taskAction;

	public TaskActionCallable(TaskActionIdImpl id, Document actionInput) {
		this.id = id;
		this.actionInput = actionInput;
	}

	@Override
	public TaskActionState call() throws PlatformException {
		 return null;

	}

}