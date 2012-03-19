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
package org.apache.ode.runtime.interpreter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.ode.runtime.exec.JAXBRuntimeUtil;
import org.apache.ode.runtime.exec.test.xml.InstructionTest;
import org.apache.ode.spi.exec.Component.InstructionSet;
import org.apache.ode.spi.exec.instruction.Instruction.Return;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class InstructionExecTest {
	private static final Logger log = Logger.getLogger(InstructionExecTest.class.getName());

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void variable() throws Exception {
	}

	@Test
	public void instruction() throws Exception {
		Set<InstructionSet> set = new HashSet<InstructionSet>();
		set.add(new InstructionSet(null, "org.apache.ode.runtime.exec.test.xml", null));
		JAXBContext ctx = JAXBRuntimeUtil.executableJAXBContextByPath(set);
		Unmarshaller um = ctx.createUnmarshaller();
		// um.setProperty("com.sun.xml.bind.ObjectFactory", new
		// TestObjectFactory());
		JAXBRuntimeUtil.setObjectFactories(um, new Object[] { new TestObjectFactory() });
		Object o = um.unmarshal(getClass().getResourceAsStream("/test/instruction.xml"));
		assertTrue(o instanceof InstructionTest);
		assertTrue(o instanceof TestInstruction);
		TestInstruction it = (TestInstruction) o;
		assertEquals("arg1", it.getArg1());
		Return r = it.execute(null);
		assertNotNull(r);
	}

	@Test
	public void control() throws Exception {
	}

	@Test
	public void joint() throws Exception {
	}

}
