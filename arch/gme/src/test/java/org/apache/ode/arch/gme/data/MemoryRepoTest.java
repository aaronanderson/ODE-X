package org.apache.ode.arch.gme.data;

import org.apache.ode.arch.gme.GuiceExternalResource;
import org.apache.ode.data.memory.repo.FileRepoManager;
import org.apache.ode.di.guice.core.JSR250Module;
import org.apache.ode.di.guice.jcache.JCacheModule;
import org.apache.ode.di.guice.memory.MemoryRepoModule;
import org.apache.ode.test.data.repo.RepoTest;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

@RunWith(Suite.class)
@SuiteClasses({ RepoTest.class })
public class MemoryRepoTest {
	

	@ClassRule
	public static TestRepoGuiceExternalResource resource = new TestRepoGuiceExternalResource((new TestRepoModule()));
	
	
	public static class TestRepoGuiceExternalResource extends GuiceExternalResource{
		TestRepoGuiceExternalResource(Module ... modules){
			super(modules);
		}

		@Override
		protected void before() throws Throwable {
			super.before();
			FileRepoManager mgr = container.getInstance(FileRepoManager.class);
			mgr.loadFileRepository("test/filerepo/test-repo.xml");
		}
		
		
	}
	
	public static class TestRepoModule extends AbstractModule {

		protected void configure() {
			install(new JSR250Module());
			install(new JCacheModule());
			install(new MemoryRepoModule());
			//install(new DIModule(new OperationAnnotationProcessor()));

		}

	}

}
