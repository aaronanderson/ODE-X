/*
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.ode.runtime.ectx.test.xml.GetOperationTest;
import org.apache.ode.runtime.ectx.test.xml.SetOperationTest;
import org.apache.ode.runtime.ectx.test.xml.TestVariables;
import org.apache.ode.runtime.ectx.test.xml.TestVariables.TestVariable;
import org.apache.ode.runtime.exec.JAXBRuntimeUtil;
import org.apache.ode.runtime.exec.platform.ExecutableObjectFactoryImpl;
import org.apache.ode.runtime.exec.platform.ExecutionContextObjectFactoryImpl;
import org.apache.ode.runtime.exec.test.TestCtxObjectFactory.TestCtxObjectFactoryImpl;
import org.apache.ode.runtime.exec.test.TestObjectFactory.TestObjectFactoryImpl;
import org.apache.ode.runtime.exec.test.xml.InstructionTest;
import org.apache.ode.runtime.interpreter.ExecutionContextImpl;
import org.apache.ode.spi.exec.Component.InstructionSet;
import org.apache.ode.spi.exec.ExecutableObjectFactory;
import org.apache.ode.spi.exec.ExecutableScope;
import org.apache.ode.spi.exec.InstructionScope;
import org.apache.ode.spi.exec.instruction.ExecutionContextObjectFactory;
import org.apache.ode.spi.exec.instruction.xml.BlockStack;
import org.apache.ode.spi.exec.instruction.xml.ExecutionState;
import org.apache.ode.spi.exec.xml.Block;
import org.apache.ode.spi.exec.xml.Executable;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;

public class InstructionExecTest {
	private static final Logger log = Logger.getLogger(InstructionExecTest.class.getName());
	private static Block block;
	private static Set<InstructionSet> set;
	private static Injector injector;
	private static JAXBContext ectx;
	private static JAXBContext ecctx;
	private static Schema eschema;
	private static Schema ectxschema;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		set = new HashSet<InstructionSet>();
		set.add(new InstructionSet(null, "org.apache.ode.runtime.exec.test.xml", TestObjectFactoryImpl.class, "org.apache.ode.runtime.ectx.test.xml",
				TestCtxObjectFactoryImpl.class));
		ectx = JAXBRuntimeUtil.executableJAXBContextByPath(set);
		ecctx = JAXBRuntimeUtil.executionContextJAXBContextByPath(set);
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		eschema = schemaFactory.newSchema(new Source[] { new StreamSource(cl.getResourceAsStream("META-INF/xsd/executable.xsd")),
				new StreamSource(cl.getResourceAsStream("META-INF/xsd/test.xsd")) });
		ectxschema = schemaFactory.newSchema(new Source[] { new StreamSource(cl.getResourceAsStream("META-INF/xsd/execution-context.xsd")),
				new StreamSource(cl.getResourceAsStream("META-INF/xsd/test-context.xsd")) });

		Unmarshaller eum = ectx.createUnmarshaller();

		eum.setSchema(eschema);
		injector = Guice.createInjector(new InstructionExecTestModule());
		TestObjectFactory testExecutableOF = injector.getInstance(TestObjectFactory.class);
		ExecutableObjectFactory execExecutableOF = injector.getInstance(ExecutableObjectFactory.class);
		JAXBRuntimeUtil.setObjectFactories(eum, new Object[] { execExecutableOF, testExecutableOF });
		Object o = eum.unmarshal(InstructionExecTest.class.getResourceAsStream("/test/instruction.xml"));
		assertTrue(o instanceof JAXBElement);
		o = ((JAXBElement<?>) o).getValue();
		assertTrue(o instanceof Executable);
		Executable e = (Executable) o;
		assertEquals(1, e.getBlock().size());
		block = e.getBlock().get(0);
		assertTrue(block.getInstructions().size() > 0);

	}

	public static class InstructionExecTestModule extends AbstractModule {
		@Override
		protected void configure() {
			bindScope(InstructionScope.class, new Scope() {

				@Override
				public <T> Provider<T> scope(Key<T> arg0, Provider<T> arg1) {
					return arg1;
				}
			});
			bindScope(ExecutableScope.class, new Scope() {

				@Override
				public <T> Provider<T> scope(Key<T> arg0, Provider<T> arg1) {
					return arg1;
				}
			});

			bind(InstructionTest.class).to(TestInstruction.class);
			bind(SetOperationTest.class).to(SetTestOperation.class);
			bind(GetOperationTest.class).to(GetTestOperation.class);
			bind(TestObjectFactory.class).to(TestObjectFactoryImpl.class);
			bind(TestCtxObjectFactory.class).to(TestCtxObjectFactoryImpl.class);
			bind(ExecutableObjectFactory.class).to(ExecutableObjectFactoryImpl.class);
			bind(ExecutionContextObjectFactory.class).to(ExecutionContextObjectFactoryImpl.class);
		}
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
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
		ExecutionState state = new ExecutionState();
		state.setStack(of.createBlock(of.createBlockStack()));
		TestVariables vars = new TestVariables();
		state.getStack().getValue().getAny().add(vars);
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
		ExecutionState state = new ExecutionState();
		ExecutionContextImpl impl = new ExecutionContextImpl(state);
		ExecutionContextObjectFactory of = injector.getInstance(ExecutionContextObjectFactory.class);
		impl.push(of.createBlock(of.createBlockStack()));
		it.execute(impl);
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
