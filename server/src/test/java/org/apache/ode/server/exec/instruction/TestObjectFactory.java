package org.apache.ode.server.exec.instruction;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.xml.bind.annotation.XmlRegistry;

import org.apache.ode.server.test.xml.AttributeTest;
import org.apache.ode.server.test.xml.ObjectFactory;
import org.apache.ode.server.test.xml.ScopeTest;
import org.apache.ode.spi.exec.ExecutableScope;


@XmlRegistry
@ExecutableScope
public class TestObjectFactory extends ObjectFactory {
	
	@Inject
	Provider<AttributeInstruction> tap;
	@Inject
	Provider<ScopeInstruction> tsp;
	

	
	@Override
	public ScopeTest createScopeTest() {
		return tsp.get();
	}



	@Override
	public AttributeTest createAttributeTest() {
		return tap.get();
	}



	
	

}
