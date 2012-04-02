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

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;

import org.apache.ode.runtime.exec.JAXBRuntimeUtil;
import org.apache.ode.runtime.exec.platform.ExecutableObjectFactoryImpl;
import org.apache.ode.runtime.exec.platform.ExecutionContextObjectFactoryImpl;
import org.apache.ode.runtime.exec.test.TestCtxObjectFactory.TestCtxObjectFactoryImpl;
import org.apache.ode.runtime.exec.test.TestObjectFactory.TestObjectFactoryImpl;
import org.apache.ode.runtime.exec.test.xml.InstructionTest;
import org.apache.ode.runtime.interpreter.ExecutionContextImpl;
import org.apache.ode.spi.exec.Component.InstructionSet;
import org.apache.ode.spi.exec.ExecutableObjectFactory;
import org.apache.ode.spi.exec.instruction.ExecutionContextObjectFactory;
import org.apache.ode.spi.exec.xml.Block;
import org.apache.ode.spi.exec.xml.Executable;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class InstructionExecTest {
	private static final Logger log = Logger.getLogger(InstructionExecTest.class.getName());
	private static Block block;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Set<InstructionSet> set = new HashSet<InstructionSet>();
		set.add(new InstructionSet(null, "org.apache.ode.runtime.exec.test.xml", null, "org.apache.ode.runtime.exec.test.xml", null));
		JAXBContext ectx = JAXBRuntimeUtil.executableJAXBContextByPath(set);
		//SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		//Schema schema = schemaFactory.newSchema(new Source[] {new StreamSource("file:///c:/dev/ode/ODE-X/spi/src/main/resources/META-INF/xsd/executable.xsd"),new StreamSource("file:///c:/dev/ode/ODE-X/runtime/src/test/resources/META-INF/xsd/test.xsd")}); 

		//JAXBContext ctx = JAXBContext.newInstance("org.apache.ode.runtime.exec.test.xml:org.apache.ode.spi.exec.xml");
		//    JAXBContext ctx = JAXBContext.newInstance("org.apache.ode.runtime.exec.test.xml");
		Unmarshaller eum = ectx.createUnmarshaller();
		//um.setSchema(schema);
		// um.setProperty("com.sun.xml.bind.ObjectFactory", new
		// TestObjectFactory());
		Injector injector = Guice.createInjector(new InstructionExecTestModule());
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
	public void instruction() throws Exception {
		Object i = block.getInstructions().get(0);
		assertNotNull(i);
		assertTrue(i instanceof JAXBElement);
		i = ((JAXBElement<?>) i).getValue();
		assertTrue(i instanceof InstructionTest);
		assertTrue(i instanceof TestInstruction);
		TestInstruction it = (TestInstruction) i;
		assertEquals("arg1", it.getArg1());
		ExecutionContextImpl impl = new ExecutionContextImpl();
		it.execute(impl);
		//assertNotNull(r);
	}

	@Test
	public void control() throws Exception {
	}

	@Test
	public void joint() throws Exception {
	}

}
