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
package org.apache.ode.runtime.exec.executable;

/*
 * #%L
 * ODE Runtime
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2012 ODE
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.google.inject.AbstractModule;

public class ExchangeOperationTest extends ExecTestBase {

	public static final String TEST_NS = "http://ode.apache.org/ExchangeTest";

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		setup("/test/exchange.xml", new ExchangeExecTestModule());
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		teardown();
	}

	public static class ExchangeExecTestModule extends AbstractModule {
		@Override
		protected void configure() {
			//bind(InstructionTest.class).to(TestInstruction.class);
			//bind(SetOperationTest.class).to(SetTestOperation.class);
			//bind(GetOperationTest.class).to(GetTestOperation.class);
		}
	}

}