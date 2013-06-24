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
package org.apache.ode.tests.bpel;

import org.apache.ode.cli.CLI;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.eviware.soapui.tools.SoapUITestCaseRunner;

public class HelloWorldIT {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		StringBuilder output = new StringBuilder();
		if (!CLI.execute(output, "--port", "9696", "import", "--verbose", "--file", "target/test-classes/bpel/HelloWorld/HelloWorld.wsdl")) {
			throw new Exception(output.toString());
		}
		if (!CLI.execute(output, "--port", "9696", "import", "--verbose", "--file", "target/test-classes/bpel/HelloWorld/HelloWorld.bpel")) {
			throw new Exception(output.toString());
		}

		if (!CLI.execute(output, "--port", "9696", "build", "--verbose", "--file", "target/test-classes/bpel/HelloWorld/HelloWorld.build")) {
			throw new Exception(output.toString());
		}

		if (!CLI.execute(output, "--port", "9696", "setup", "--verbose", "--name", "{http://ode/bpel/unit-test}HelloWorld", "--file", "target/installData.xml")) {
			throw new Exception(output.toString());
		}

		if (!CLI.execute(output, "--port", "9696", "install", "--verbose", "--name", "{http://ode/bpel/unit-test}HelloWorld", "--file",
				"target/installData.xml")) {
			throw new Exception(output.toString());
		}

		if (!CLI.execute(output, "--port", "9696", "start", "--verbose", "--name", "{http://ode/bpel/unit-test}HelloWorld")) {
			throw new Exception(output.toString());
		}
		/*
		 * if (!CLI.execute(output, "--port", "9696", "install", "--name",
		 * "{http://ode/bpel/unit-test}HelloWorld")) { throw new
		 * Exception(output.toString()); }
		 */
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void testHelloWorld() throws Exception {
		SoapUITestCaseRunner runner = new SoapUITestCaseRunner();
		// runner.setEndpoint(endpoint);
		// runner.setProjectFile(
		// "src/test/resources/tests/helloworld/helloworld.xml" );
		// runner.run();
	}

}
