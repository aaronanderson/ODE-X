package org.apache.ode.arch.gme.runtime;

import org.apache.ode.arch.gme.TestGuiceDIContainer;
import org.apache.ode.arch.gme.GuiceExternalResource;
import org.apache.ode.di.guice.core.DIContainerModule;
import org.apache.ode.di.guice.core.JSR250Module;
import org.apache.ode.test.core.DIContainerTest;
import org.apache.ode.test.core.DIContainerTest.FieldQualifier;
import org.apache.ode.test.core.DIContainerTest.TestBadField;
import org.apache.ode.test.core.DIContainerTest.TestFieldQualifier;
import org.apache.ode.test.core.DIContainerTest.TestFieldQualifier1;
import org.apache.ode.test.core.DIContainerTest.TestInstance;
import org.apache.ode.test.core.DIContainerTest.TestSingleton;
import org.apache.ode.test.core.DIContainerTest.TestTypeQualifier;
import org.apache.ode.test.core.DIContainerTest.TestTypeQualifier1;
import org.apache.ode.test.core.DIContainerTest.TypeQualifier;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.google.inject.AbstractModule;

@RunWith(Suite.class)
@SuiteClasses({ DIContainerTest.class })
public class DITest {
	public static TestGuiceDIContainer container;

	@ClassRule
	public static GuiceExternalResource resource = new GuiceExternalResource((new TestDIContainerModule()));

	public static class TestDIContainerModule extends AbstractModule {

		protected void configure() {
			install(new JSR250Module());
			install(new DIContainerModule());
			bind(TestInstance.class);
			bind(TestSingleton.class);
			//TestFieldQualifier tf = new TestFieldQualifier();
			bind(TestFieldQualifier.class).annotatedWith(FieldQualifier.class).to(TestFieldQualifier1.class);
			bind(TestTypeQualifier.class).annotatedWith(TypeQualifier.class).to(TestTypeQualifier1.class);
			//bind(TestBadField.class); will throw exception at bind time
		}

	}

}
