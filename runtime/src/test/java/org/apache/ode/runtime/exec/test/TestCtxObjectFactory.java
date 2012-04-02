package org.apache.ode.runtime.exec.test;

import javax.xml.bind.annotation.XmlRegistry;

import org.apache.ode.runtime.exec.test.xml.InstructionTest;
import org.apache.ode.runtime.exec.test.xml.ObjectFactory;

@XmlRegistry
public abstract class TestCtxObjectFactory extends ObjectFactory {

	@XmlRegistry
	public static class TestCtxObjectFactoryImpl extends TestCtxObjectFactory {

		@Override
		public InstructionTest createInstructionTest() {
			return new TestInstruction();
		}
	}

}
