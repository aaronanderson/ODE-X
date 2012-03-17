package org.apache.ode.server.test.instruction;

import javax.inject.Provider;
import javax.xml.bind.annotation.XmlRegistry;

import org.apache.ode.server.test.xml.InstructionTest;
import org.apache.ode.server.test.xml.ObjectFactory;


@XmlRegistry
public class TestObjectFactory extends ObjectFactory {
	
	Provider<TestInstruction> tip;
	

	@Override
	public InstructionTest createInstructionTest() {
		return tip.get();
	}
	
	

}
