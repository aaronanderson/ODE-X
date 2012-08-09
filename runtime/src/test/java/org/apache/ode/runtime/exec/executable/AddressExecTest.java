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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.apache.ode.runtime.exec.executable.test.xml.AddressTest;
import org.apache.ode.runtime.interpreter.IndexedBlockAddress;
import org.apache.ode.runtime.interpreter.IndexedInstructionAddress;
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
		assertNotNull(eIndex.blocks);
		assertEquals(eIndex.blocks.length,4);
		IndexedBlockAddress b0 = eIndex.blocks[0];
		assertNotNull(b0);
		assertEquals(b0.addresses.length,1);
		IndexedInstructionAddress b0i0= b0.addresses[0];
		assertNotNull(b0i0);
		assertTrue(b0i0.ref instanceof AddressTest);
		IndexedBlockAddress b1 = eIndex.blocks[1];
		assertNotNull(b1);
		assertEquals(b1.addresses.length,1);
		IndexedInstructionAddress b1i0= b1.addresses[0];
		assertNotNull(b1i0);
		assertTrue(b1i0.ref instanceof AddressTest);
		IndexedBlockAddress b2 = eIndex.blocks[2];
		assertNotNull(b2);
		assertEquals(b2.addresses.length,1);
		IndexedInstructionAddress b2i0= b2.addresses[0];
		assertNotNull(b2i0);
		assertTrue(b2i0.ref instanceof AddressTest);
		IndexedBlockAddress b3 = eIndex.blocks[1];
		assertNotNull(b3);
		assertEquals(b3.addresses.length,1);
		IndexedInstructionAddress b3i0= b3.addresses[0];
		assertNotNull(b3i0);
		assertTrue(b3i0.ref instanceof AddressTest);

		assertSame(((AddressTest)b0i0.ref).getBref(),b1);
		assertSame(((AddressTest)b0i0.ref).getIref(),b1i0);
		assertSame(((AddressTest)b1i0.ref).getBref(),b0);
		assertSame(((AddressTest)b1i0.ref).getIref(),b0i0);
		assertSame(((AddressTest)b2i0.ref).getBref(),b2);
		assertSame(((AddressTest)b2i0.ref).getIref(),b2i0);

		
		//assertTrue(i instanceof JAXBElement);
		//i = ((JAXBElement<?>) i).getValue();
		//assertTrue(i instanceof AddressTest);
		//AddressTest at = (AddressTest) i;
		//assertEquals("b1", at.getBref());
		//assertEquals("b1i0", at.getIns());
	}

}
