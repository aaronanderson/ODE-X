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

import java.util.logging.Logger;

import org.apache.ode.spi.exec.instruction.Instruction;
import org.apache.ode.spi.exec.instruction.Instruction.ExecutionContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ExecutableTest {
	private static final Logger log = Logger.getLogger(ExecutableTest.class.getName());

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	public static class TestInstruction implements Instruction<ExecutionContext> {

		@Override
		public Return  execute(ExecutionContext execCtx) {
			return null;

		}

	}

	@Test
	public void variable() throws Exception {
	}

	@Test
	public void instruction() throws Exception {
	}

	@Test
	public void control() throws Exception {
	}

	@Test
	public void joint() throws Exception {
	}

}
