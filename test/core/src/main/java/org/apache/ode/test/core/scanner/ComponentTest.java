package org.apache.ode.test.core.scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.ode.spi.di.ComponentAnnotationScanner;
import org.apache.ode.spi.di.ComponentAnnotationScanner.ComponentModel;
import org.apache.ode.spi.di.ComponentAnnotationScanner.Components;
import org.apache.ode.spi.exec.Component;
import org.apache.ode.spi.exec.Component.EventSet;
import org.apache.ode.spi.exec.Component.EventSets;
import org.apache.ode.spi.exec.Component.ExecutableSet;
import org.apache.ode.spi.exec.Component.ExecutableSets;
import org.apache.ode.spi.exec.Component.ExecutionConfigSet;
import org.apache.ode.spi.exec.Component.ExecutionConfigSets;
import org.apache.ode.spi.exec.Component.ExecutionContextSet;
import org.apache.ode.spi.exec.Component.ExecutionContextSets;
import org.apache.ode.spi.exec.Component.Offline;
import org.apache.ode.spi.exec.Component.Online;
import org.apache.ode.test.core.TestDIContainer;
import org.junit.BeforeClass;
import org.junit.Test;

public class ComponentTest {
	
	static ComponentRegistry registry;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		TestDIContainer container = TestDIContainer.CONTAINER.get();
		registry= container.getInstance(ComponentRegistry.class);
	}

	@Test
	public void testComponentScan() throws Exception {
		//TODO test DIContainer model map injection
		ComponentAnnotationScanner cas = new ComponentAnnotationScanner();

		assertNotNull(cas);
		assertNull(cas.scan(NonComponent.class));
		ComponentModel cm = cas.scan(TestComponent.class);
		assertNotNull(cm);
		assertNotNull(cm.getTargetClass());
		assertEquals("TestComponent",cm.getName());
		assertNotNull(cm.getEventSets());
		assertNotNull(cm.getExecutableSets());
		assertNotNull(cm.getExecutionConfigSets());
		assertNotNull(cm.getExecutionContextSets());
		assertNotNull(cm.getOnline());
		assertNotNull(cm.getOffline());
		//assertFalse();
	}
	
	
	@Test
	public void testComponentRegistry() throws Exception {
		assertNotNull(registry);
		assertNotNull(registry.models);
		assertTrue(registry.models.containsKey(TestComponent.class));
	}

	public static class NonComponent {

	}

	@Component
	public static class TestComponent {

		@ExecutableSets
		public List<ExecutableSet> executableSets() {
			return new ArrayList<ExecutableSet>();

		}

		@ExecutionContextSets
		public List<ExecutionContextSet> executionContextSets() {
			return new ArrayList<ExecutionContextSet>();
		}

		@EventSets
		public List<EventSet> eventSets() {
			return new ArrayList<EventSet>();
		}

		@ExecutionConfigSets
		public List<ExecutionConfigSet> executionConfigSets() {
			return new ArrayList<ExecutionConfigSet>();
		}

		@Online
		public void online() {
		}

		@Offline
		public void offline() {
		}
	}
	
	public static class ComponentRegistry {
		
		@Inject
		@Components
		Map<Class<?>, ComponentModel> models;
		
	}
}
