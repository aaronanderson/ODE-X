package org.apache.ode.spi.deployment;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.ignite.igfs.IgfsPath;
import org.apache.ode.spi.deployment.Assembler.AssemblyException;
import org.apache.ode.spi.deployment.Deployment.DeploymentBuilder;
import org.apache.ode.spi.deployment.Deployment.Entry;

public interface AssemblyManager {

	public static final String SERVICE_NAME = "urn:org:apache:ode:assembly";

	public static final IgfsPath ASSEMBLY_DIR = new IgfsPath("/assemblies");

	public <C> void create(AssemblyDeployment<C> assembly) throws AssemblyException;

	public <C> AssemblyDeployment<C> export(URI reference) throws AssemblyException;

	public <C> void update(URI reference, C file) throws AssemblyException;

	public void updateFiles(URI reference, List<Entry> files) throws AssemblyException;

	public void assemble(URI reference) throws AssemblyException;

	public void delete(URI reference) throws AssemblyException;

	public void deploy(URI reference) throws AssemblyException;

	public void undeploy(URI reference) throws AssemblyException;

	public void createAlias(String alias, URI reference) throws AssemblyException;

	public void deleteAlias(String alias) throws AssemblyException;

	public URI alias(String alias) throws AssemblyException;

	public static class AssemblyDeployment<C> extends Deployment<C> {

		private AssemblyDeployment() {

		}

		protected URI type;
		protected URI reference;
		protected Set<URI> dependencies;

		public URI type() {
			return type;
		}

		public URI reference() {
			return reference;
		}

		public Set<URI> dependencies() {
			return dependencies != null ? dependencies : Collections.EMPTY_SET;
		}

	}

	public static class AssemblyDeploymentBuilder<C> extends DeploymentBuilder<AssemblyDeploymentBuilder<C>, AssemblyDeployment<C>, C> {

		public static <C> AssemblyDeploymentBuilder<C> instance() {
			return new AssemblyDeploymentBuilder<C>();
		}

		public AssemblyDeploymentBuilder() {
			this.deployment = new AssemblyDeployment<>();
		}

		public AssemblyDeploymentBuilder<C> type(URI type) {
			deployment.type = type;
			return this;
		}

		public AssemblyDeploymentBuilder<C> reference(URI reference) {
			deployment.reference = reference;
			return this;
		}

		public AssemblyDeploymentBuilder<C> dependencies(Set<URI> dependencies) {
			deployment.dependencies = dependencies;
			return this;
		}
	}

}
