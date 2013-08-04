package org.apache.ode.test.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Singleton;
import javax.xml.namespace.QName;

import org.apache.ode.spi.runtime.Component;
import org.apache.ode.spi.runtime.Node;
import org.apache.ode.spi.runtime.PlatformException;
import org.apache.ode.spi.runtime.Component.EventSet;
import org.apache.ode.spi.runtime.Component.EventSets;
import org.apache.ode.spi.runtime.Component.ExecutableSet;
import org.apache.ode.spi.runtime.Component.ExecutableSets;
import org.apache.ode.spi.runtime.Component.ExecutionConfigSet;
import org.apache.ode.spi.runtime.Component.ExecutionConfigSets;
import org.apache.ode.spi.runtime.Component.ExecutionContextSet;
import org.apache.ode.spi.runtime.Component.ExecutionContextSets;
import org.apache.ode.spi.runtime.Component.Offline;
import org.apache.ode.spi.runtime.Component.Online;
import org.apache.ode.spi.runtime.Node.NodeStatus;
import org.apache.ode.test.core.TestDIContainer;
import org.junit.BeforeClass;
import org.junit.Test;

public class NodeTest {

	protected static Node node;
	protected static TestComponent tc;
	protected static TestNodeListener nl;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		TestDIContainer container = TestDIContainer.CONTAINER.get();
		assertNotNull(container);
		//setupDIContainer(RepoTest.class);
		node = container.getInstance(Node.class);
		assertNotNull(node);
		tc = container.getInstance(TestComponent.class);
		assertNotNull(tc);
		nl = container.getInstance(TestNodeListener.class);
		assertNotNull(nl);

	}

	//@AfterClass
	//public static void tearDownAfterClass() throws Exception {

	//}

	@Test
	public void testComponent() throws Exception {
		assertFalse(tc.online);
		node.online();
		assertTrue(tc.online);
		assertEquals(1, node.eventSets().size());
		assertEquals("Event", node.eventSets().entrySet().iterator().next().getKey().getLocalPart());
		assertEquals(1, node.executableSets().size());
		assertEquals("Executable", node.executableSets().entrySet().iterator().next().getKey().getLocalPart());
		assertEquals(1, node.executionConfigSets().size());
		assertEquals("ExecutionConfig", node.executionConfigSets().entrySet().iterator().next().getKey().getLocalPart());
		assertEquals(1, node.executionContextSets().size());
		assertEquals("ExecutionContext", node.executionContextSets().entrySet().iterator().next().getKey().getLocalPart());
		node.offline();
		assertFalse(tc.online);

	}

	@Test
	public void testNodeListener() throws Exception {
		assertFalse(nl.online);
		node.online();
		assertTrue(nl.online);
		node.offline();
		assertFalse(nl.online);

	}

	@Component
	@Singleton
	public static class TestComponent {

		public boolean online = false;
		public static final ThreadLocal<Boolean> throwOnlineError = new ThreadLocal<Boolean>();
		public static final ThreadLocal<Boolean> throwOfflineError = new ThreadLocal<Boolean>();

		@EventSets
		public Set<EventSet> eventSets() {
			Set<EventSet> set = new HashSet<EventSet>();
			set.add(new EventSet(new QName("Event"), "", null));
			return set;
		}

		@ExecutableSets
		public Set<ExecutableSet> executableSets() {
			Set<ExecutableSet> set = new HashSet<ExecutableSet>();
			set.add(new ExecutableSet(new QName("Executable"), "", null));
			return set;
		}

		@ExecutionConfigSets
		public Set<ExecutionConfigSet> execConfigSets() {
			Set<ExecutionConfigSet> set = new HashSet<ExecutionConfigSet>();
			set.add(new ExecutionConfigSet(new QName("ExecutionConfig"), "", null));
			return set;
		}

		@ExecutionContextSets
		public Set<ExecutionContextSet> execContextSets() {
			Set<ExecutionContextSet> set = new HashSet<ExecutionContextSet>();
			set.add(new ExecutionContextSet(new QName("ExecutionContext"), "", null));
			return set;
		}

		@Online
		public void online() throws PlatformException {
			if (throwOnlineError.get() != null && throwOnlineError.get()) {
				throw new PlatformException("Online Error");
			}
			online = true;
		}

		@Offline
		public void offline() throws PlatformException {
			if (throwOfflineError.get() != null && throwOfflineError.get()) {
				throw new PlatformException("Offline Error");
			}
			online = false;
		}
	}

	@NodeStatus
	@Singleton
	public static class TestNodeListener {

		public boolean online = false;
		public static final ThreadLocal<Boolean> throwOnlineError = new ThreadLocal<Boolean>();
		public static final ThreadLocal<Boolean> throwOfflineError = new ThreadLocal<Boolean>();

		@org.apache.ode.spi.runtime.Node.Online
		public void online() throws PlatformException {
			if (throwOnlineError.get() != null && throwOnlineError.get()) {
				throw new PlatformException("Online Error");
			}
			online = true;
		}

		@org.apache.ode.spi.runtime.Node.Offline
		public void offline() throws PlatformException {
			if (throwOfflineError.get() != null && throwOfflineError.get()) {
				throw new PlatformException("Offline Error");
			}
			online = false;
		}

	}
}
