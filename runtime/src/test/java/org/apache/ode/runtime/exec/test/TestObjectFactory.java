package org.apache.ode.runtime.exec.test;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.xml.bind.annotation.XmlRegistry;

import org.apache.ode.runtime.exec.test.xml.InstructionTest;
import org.apache.ode.runtime.exec.test.xml.ObjectFactory;

@XmlRegistry
public abstract class TestObjectFactory extends ObjectFactory {

	@XmlRegistry
	public static class TestObjectFactoryImpl extends TestObjectFactory {
		@Inject
		Provider<TestInstruction> testProvider;
		
		@Override
		public InstructionTest createInstructionTest() {
			return testProvider.get();
		}
	}

}

