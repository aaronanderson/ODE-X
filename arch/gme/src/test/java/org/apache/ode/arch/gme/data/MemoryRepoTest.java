package org.apache.ode.arch.gme.data;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.cache.expiry.Duration;

import org.apache.ode.arch.gme.GuiceExternalResource;
import org.apache.ode.data.core.repo.RepoFileTypeMap;
import org.apache.ode.data.memory.repo.FileRepository;
import org.apache.ode.data.memory.repo.xml.IndexMode;
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

	public static class TestRepoGuiceExternalResource extends GuiceExternalResource {
		TestRepoGuiceExternalResource(Module... modules) {
			super(modules);
		}

		@Override
		protected void before() throws Throwable {
			super.before();
			Path base = Files.createDirectories(Paths.get("target/test/file-repo"));
			Files.copy(Thread.currentThread().getContextClassLoader().getResourceAsStream("test/file-repo/testFS.foo"), base.resolve("testFS.foo"));
			RepoFileTypeMap map = container.getInstance(RepoFileTypeMap.class);
			map.registerFileExtension("foo", "application/foo");
			FileRepository frepo = container.getInstance(FileRepository.class);
			frepo.loadRepositoryCache("test/file-repo/file-repo.xml");

		}

	}

	public static class TestRepoModule extends AbstractModule {

		protected void configure() {
			install(new JSR250Module());
			install(new JCacheModule().withFileRepoMode(IndexMode.TRANSIENT).withDuration(Duration.ONE_MINUTE));
			install(new MemoryRepoModule());
			//install(new DIModule(new OperationAnnotationProcessor()));

		}

	}

}
