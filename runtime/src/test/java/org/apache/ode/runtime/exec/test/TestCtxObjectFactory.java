package org.apache.ode.runtime.exec.test;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.bind.annotation.XmlSeeAlso;

import org.apache.ode.runtime.ectx.test.xml.GetOperationTest;
import org.apache.ode.runtime.ectx.test.xml.ObjectFactory;
import org.apache.ode.runtime.ectx.test.xml.SetOperationTest;

@XmlRegistry
public abstract class TestCtxObjectFactory extends ObjectFactory {

	@XmlRegistry
	public static class TestCtxObjectFactoryImpl extends TestCtxObjectFactory {
		@Inject
		Provider<SetOperationTest> testSetProvider;

		@Inject
		Provider<GetOperationTest> testGetProvider;
		
		@Override
		public SetOperationTest createSetOperationTest() {
			return testSetProvider.get();
		}

		@Override
		public GetOperationTest createGetOperationTest() {
			return testGetProvider.get();
		}
	}

}
