package org.apache.ode.runtime.cli;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.util.AnnotationLiteral;

import org.apache.ignite.IgniteException;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.compute.ComputeJobResult;
import org.apache.ignite.compute.ComputeTaskAdapter;
import org.apache.ignite.compute.ComputeTaskName;
import org.apache.ode.spi.cli.CLICommand;
import org.apache.ode.spi.cli.CommandJob;
import org.apache.ode.spi.cli.CommandRequest;
import org.apache.ode.spi.cli.CommandResponse;

@ComputeTaskName(CLITask.CLI_TASK_NAME)
public class CLITask extends ComputeTaskAdapter<CommandRequest, CommandResponse> {

	public static final String CLI_TASK_NAME = "urn:org:apache:ode:cli:exec";
	private static final long serialVersionUID = 1L;
	private CreationalContext<CommandJob> ctx;

	@Override
	public Map<? extends ComputeJob, ClusterNode> map(List<ClusterNode> subgrid, CommandRequest arg) throws IgniteException {
		// Set<Bean<?>> beans = beanManager.getBeans(Object.class,new AnnotationLiteral<Any>() {}));
		BeanManager bm = CDI.current().getBeanManager();
		Set<Bean<?>> beans = bm.getBeans(CommandJob.class, new CLICommandLiteral(arg.entity(), arg.name()));

		Bean<CommandJob> bean = (Bean<CommandJob>) beans.iterator().next();
		ctx = bm.createCreationalContext(bean);
		CommandJob commandJob = (CommandJob) bm.getReference(bean, CommandJob.class, ctx);
		commandJob.request(arg);

		return Collections.singletonMap(commandJob, subgrid.get(0));
	}

	@Override
	public CommandResponse reduce(List<ComputeJobResult> results) throws IgniteException {
		// TODO Auto-generated method stub
		ctx.release();
		return results.get(0).getData();
	}

	private class CLICommandLiteral extends AnnotationLiteral<CLICommand> implements CLICommand {
		private static final long serialVersionUID = 1L;

		private final String entity;
		private final String name;

		private CLICommandLiteral(String entity, String name) {
			this.entity = entity;
			this.name = name;
		}

		@Override
		public String entity() {
			return entity;
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public Option[] options() {
			return new Option[0];
		}

	}

}
