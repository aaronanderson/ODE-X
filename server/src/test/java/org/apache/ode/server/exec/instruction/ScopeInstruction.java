package org.apache.ode.server.exec.instruction;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.ode.server.test.xml.ScopeTest;
import org.apache.ode.spi.exec.ExecutableScope;
import org.apache.ode.spi.exec.instruction.Instruction;
import org.apache.ode.spi.exec.instruction.Instruction.ExecutionContext;

@ExecutableScope
public class ScopeInstruction extends ScopeTest implements Instruction<ExecutionContext>{

	private boolean started=false;
	private boolean stopped=false;
	
	@Inject
	@Named("shared")
	private ExecutableShared shared=null;
	
	
	@PostConstruct
	public void start(){
		started=true;
	}
	
	@PreDestroy
	public void stop(){
		stopped=true;
	}
	
	@Override
	public Return execute(ExecutionContext execCtx) {
		return Success.success();
		
	}

	public boolean isStarted() {
		return started;
	}

	public boolean isStopped() {
		return stopped;
	}

	public ExecutableShared getShared() {
		return shared;
	}
	
	
	
}


