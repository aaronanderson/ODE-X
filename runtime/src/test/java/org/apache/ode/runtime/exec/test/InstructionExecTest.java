/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ode.runtime.exec.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;

import org.apache.ode.runtime.ectx.test.xml.GetOperationTest;
import org.apache.ode.runtime.ectx.test.xml.SetOperationTest;
import org.apache.ode.runtime.ectx.test.xml.TestVariables;
import org.apache.ode.runtime.ectx.test.xml.TestVariables.TestVariable;
import org.apache.ode.runtime.exec.platform.ExecutableObjectFactoryImpl;
import org.apache.ode.runtime.exec.platform.ExecutionContextObjectFactoryImpl;
import org.apache.ode.runtime.exec.platform.ScopeContext.InstructionScopeContext;
import org.apache.ode.runtime.exec.test.TestCtxObjectFactory.TestCtxObjectFactoryImpl;
import org.apache.ode.runtime.exec.test.TestObjectFactory.TestObjectFactoryImpl;
import org.apache.ode.runtime.exec.test.xml.InstructionTest;
import org.apache.ode.runtime.interpreter.ExecutionContextImpl;
import org.apache.ode.spi.exec.ExecutableObjectFactory;
import org.apache.ode.spi.exec.ExecutableScope;
import org.apache.ode.spi.exec.InstructionScope;
import org.apache.ode.spi.exec.instruction.ExecutionContextObjectFactory;
import org.apache.ode.spi.exec.instruction.xml.ExecutionState;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;

public class InstructionExecTest extends ExecTestBase {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		setup("/test/instruction.xml", new InstructionExecTestModule());
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		teardown();
	}

	public static class InstructionExecTestModule extends AbstractModule {
		@Override
		protected void configure() {
			bind(InstructionTest.class).to(TestInstruction.class);
			bind(SetOperationTest.class).to(SetTestOperation.class);
			bind(GetOperationTest.class).to(GetTestOperation.class);
		}
	}

	@Test
	public void variable() throws Exception {
	}

	@Test
	public void JAXBListMap() throws Exception {
		Marshaller m = ecctx.createMarshaller();
		m.setSchema(ectxschema);
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		ExecutionContextObjectFactory of = injector.getInstance(ExecutionContextObjectFactory.class);
		ExecutionState state = of.createExecutionState();
		state.setStatus(of.createRunning(of.createRunning()));
		state.setBlock(of.createBlockStack());
		TestVariables vars = new TestVariables();
		state.getBlock().getAny().add(vars);
		TestVariable var1 = new TestVariable();
		var1.setName("test1");
		var1.setValue("test2");
		TestVariable var2 = new TestVariable();
		var2.setName("test3");
		var2.setValue("test4");
		vars.getVariables().add(var1);
		vars.getVariables().add(var2);
		assertTrue(vars.getVariables() instanceof TestMap);
		Map<String, TestVariable> map = ((TestMap<TestVariable>) vars.getVariables()).mapView();
		var1 = map.get("test1");
		assertNotNull(var1);
		assertEquals("test2", var1.getValue());
		var2 = map.get("test3");
		assertNotNull(var2);
		assertEquals("test4", var2.getValue());
		StringWriter writer = new StringWriter();
		m.marshal(of.createExecutionState(state), writer);
		System.out.println(writer.toString());
	}

	@Test
	public void instruction() throws Exception {
		Object i = block.getInstructions().get(0);
		assertNotNull(i);
		assertTrue(i instanceof JAXBElement);
		i = ((JAXBElement<?>) i).getValue();
		assertTrue(i instanceof InstructionTest);
		assertTrue(i instanceof TestInstruction);
		TestInstruction it = (TestInstruction) i;
		assertEquals("arg1", it.getArg1());
		ExecutionContextObjectFactory of = injector.getInstance(ExecutionContextObjectFactory.class);
		ExecutionState state = of.createExecutionState();
		state.setStatus(of.createRunning(of.createRunning()));
		ExecutionContextImpl impl = new ExecutionContextImpl(state);
		impl.pushBlock(of.createBlockStack());
		InstructionScopeContext isc = injector.getInstance(InstructionScopeContext.class);
		isc.create();
		try {
			isc.begin();
			it.execute(impl);
		} finally {
			isc.end();
			isc.destroy();
		}

		//impl.pop();
		//assertNotNull(r);
		//make sure state is marshalable
		StringWriter writer = new StringWriter();
		Marshaller m = ecctx.createMarshaller();
		m.setSchema(ectxschema);
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		m.marshal(of.createExecutionState(state), writer);
		//System.out.println(writer.toString());
	}

	@Test
	public void control() throws Exception {
	}

	@Test
	public void joint() throws Exception {
	}

}
