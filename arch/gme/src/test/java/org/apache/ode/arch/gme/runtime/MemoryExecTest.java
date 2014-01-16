package org.apache.ode.arch.gme.runtime;

import javax.xml.namespace.QName;

import org.apache.ode.arch.gme.GuiceExternalResource;
import org.apache.ode.arch.gme.RuntimeConfigModule;
import org.apache.ode.arch.gme.TestGuiceDIContainer;
import org.apache.ode.di.guice.core.DIContainerModule;
import org.apache.ode.di.guice.core.JSR250Module;
import org.apache.ode.di.guice.memory.runtime.NodeModule;
import org.apache.ode.di.guice.memory.runtime.WorkModule;
import org.apache.ode.di.guice.runtime.DIDiscoveryModule;
import org.apache.ode.runtime.core.node.NodeBase.Architecture;
import org.apache.ode.runtime.memory.work.xml.WorkConfig;
import org.apache.ode.runtime.memory.work.xml.WorkExec;
import org.apache.ode.runtime.memory.work.xml.WorkScheduler;
import org.apache.ode.test.runtime.exec.ExecTest;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.google.inject.AbstractModule;

@RunWith(Suite.class)
@SuiteClasses({ ExecTest.class })
//@SuiteClasses({ OperationTest.class })
public class MemoryExecTest {
	public static TestGuiceDIContainer container;

	@ClassRule
	public static GuiceExternalResource resource = new GuiceExternalResource((new TestDIContainerModule()));

	public static class TestDIContainerModule extends AbstractModule {

		protected void configure() {
			install(new JSR250Module());
			install(new DIContainerModule());
			install(new DIDiscoveryModule());
			bind(QName.class).annotatedWith(Architecture.class).toInstance(RuntimeConfigModule.GME_ARCHITECTURE);
			install(new NodeModule());
			bind(WorkConfig.class).toInstance(new WorkConfig().withWorkExec(new WorkExec()).withWorkScheduler(new WorkScheduler()));
			install(new WorkModule());

			

		}

	}

}
