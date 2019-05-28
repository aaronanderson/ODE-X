package org.apache.ode.spi.deployment;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

public interface Processor {

	public static final String CREATE_STAGE = "urn:org:apache:ode:assembly:stage:create";
	public static final String INSPECT_STAGE = "urn:org:apache:ode:assembly:stage:inspect";
	public static final String DEPLOY_STAGE = "urn:org:apache:ode:assembly:stage:deploy";
	public static final String UNDEPLOY_STAGE = "urn:org:apache:ode:assembly:stage:undeploy";
	public static final String DELETE_STAGE = "urn:org:apache:ode:assembly:stage:delete";

	public static final String[] DEFAULT_LIFECYCLE = {};

	@Inherited
	@Qualifier
	@Target({ TYPE, FIELD })
	@Retention(RUNTIME)
	public @interface Id {
		String value();

		String stage();

	}

	// TODO use compile time CDI events for lifecycle stages or runtime ignite cache entry configurations.

}
