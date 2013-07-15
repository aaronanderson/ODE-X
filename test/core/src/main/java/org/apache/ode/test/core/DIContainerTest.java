package org.apache.ode.test.core;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.inject.Singleton;

import org.apache.ode.spi.di.DIContainer;
import org.junit.BeforeClass;
import org.junit.Test;

public class DIContainerTest {

	static TestDIContainer container;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		container = TestDIContainer.CONTAINER.get();
	}

	@Test
	public void testDIContainerInjection() throws Exception {

		assertNotNull(container);
		DIContainer instance = container.getInstance(DIContainer.class);
		assertNotNull(instance);
		instance = instance.getInstance(DIContainer.class);
		assertNotNull(instance);
		//assertFalse();
	}

	@Test
	public void testGetInstance() throws Exception {

		assertNotNull(container);
		TestInstance instance = container.getInstance(TestInstance.class);
		assertNotNull(instance);
		//assertFalse();
	}

	@Test
	public void testGetSingleton() throws Exception {

		assertNotNull(container);
		TestSingleton instance1 = container.getInstance(TestSingleton.class);
		assertNotNull(instance1);
		TestSingleton instance2 = container.getInstance(TestSingleton.class);
		assertEquals(instance1, instance2);
		//assertFalse();
	}

	@Test
	public void testGetQualifier() throws Exception {

		assertNotNull(container);
		try {
			container.getInstance(TestTypeQualifier.class);
			fail();
		} catch (Exception e) {
		}
		TypeQualifier q = (TypeQualifier) Proxy.newProxyInstance(TypeQualifier.class.getClassLoader(), new Class[] { TypeQualifier.class }, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) {
				return TypeQualifier.class;
			}
		});

		assertNotNull(q);
		TestTypeQualifier instance1 = container.getInstance(TestTypeQualifier.class, q);
		assertNotNull(instance1);
		assertTrue(instance1 instanceof TestTypeQualifier1);
		assertNotNull(((TestTypeQualifier1) instance1).field);

		/*will throw exception at bind time
		try {
			container.getInstance(TestBadField.class);
			fail();
		} catch (Exception e) {
		}
		*/
	}

	public static class TestInstance {

	}

	@Singleton
	public static class TestSingleton {

	}

	@Qualifier
	@Retention(RUNTIME)
	@Target(FIELD)
	public @interface FieldQualifier {

	}

	@Qualifier
	@Retention(RUNTIME)
	@Target(TYPE)
	public @interface TypeQualifier {

	}

	public static interface TestFieldQualifier {

	}

	public static class TestFieldQualifier1 implements TestFieldQualifier {

	}

	public static interface TestTypeQualifier {

	}

	public static class TestTypeQualifier1 implements TestTypeQualifier {

		@Inject
		@FieldQualifier
		TestFieldQualifier field;
	}

	public static class TestBadField {

		@Inject
		TestFieldQualifier field;
	}

}
