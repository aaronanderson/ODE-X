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

import javax.xml.bind.JAXBElement;

import org.apache.ode.runtime.exec.test.xml.AddressTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class AddressExecTest extends ExecTestBase {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		setup("/test/address.xml");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		teardown();
	}

	@Test
	public void addressLinks() throws Exception {
		Object i = block.getInstructions().get(0);
		assertNotNull(i);
		assertTrue(i instanceof JAXBElement);
		i = ((JAXBElement<?>) i).getValue();
		assertTrue(i instanceof AddressTest);
		AddressTest at = (AddressTest) i;
		//assertEquals("b1", at.getBref());
		//assertEquals("b1i0", at.getIns());
	}

}
