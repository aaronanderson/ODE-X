package org.apache.ode.spi.cli;

import org.apache.ignite.IgniteException;
import org.apache.ignite.compute.ComputeJob;

public abstract class CommandJob implements ComputeJob {

	private CommandRequest request;

	public CommandRequest request() {
		return request;
	}

	public CommandJob request(CommandRequest request) {
		this.request = request;
		return this;
	}

	@Override
	public void cancel() {
		// TODO Auto-generated method stub
	}

	@Override
	public Object execute() throws IgniteException {
		return execute(request);
	}

	public abstract CommandResponse execute(CommandRequest request) throws IgniteException;

}
