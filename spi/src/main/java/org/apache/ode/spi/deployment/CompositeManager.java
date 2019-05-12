package org.apache.ode.spi.deployment;

import java.net.URI;

import org.apache.ignite.igfs.IgfsPath;
import org.apache.ode.spi.deployment.Deployment.DeploymentBuilder;

public interface CompositeManager {

	public static final String SERVICE_NAME = "urn:org:apache:ode:composite";

	public static final IgfsPath COMPOSITE_DIR = new IgfsPath("/composites");

	public <C> void create(CompositeDeployment<C> composite);

	public <C> CompositeDeployment<C> export(URI reference);

	public <C> void update(URI reference, C config);

	public void delete(URI reference);

	public void activate(URI reference);

	public void deactivate(URI reference);

	public void instrument(URI reference);

	public void createAlias(String alias, URI reference);

	public void deleteAlias(String alias);

	public URI alias(String alias);

	public static class CompositeDeployment<C> extends Deployment<C> {

		private CompositeDeployment() {

		}

		protected URI type;
		protected URI reference;

		public URI type() {
			return type;
		}

		public URI reference() {
			return reference;
		}

	}

	public static class CompositeDeploymentBuilder<C> extends DeploymentBuilder<CompositeDeploymentBuilder<C>, CompositeDeployment<C>, C> {

		public static <C> CompositeDeploymentBuilder<C> instance() {
			return new CompositeDeploymentBuilder<C>();
		}

		public CompositeDeploymentBuilder() {
			this.deployment = new CompositeDeployment<>();
		}

		public CompositeDeploymentBuilder<C> type(URI type) {
			deployment.type = type;
			return this;
		}

		public CompositeDeploymentBuilder<C> reference(URI reference) {
			deployment.reference = reference;
			return this;
		}
	}

}
