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
package org.apache.ode.runtime.memory.operation;

import org.apache.ode.spi.work.Operation;
import org.apache.ode.spi.work.Operation.Command;
import org.apache.ode.spi.work.Operation.OperationSet;

@OperationSet(namespace = ContextOperations.OPERATION_NAMESPACE, commandNamespace = ContextOperations.COMMAND_NAMESPACE)
public class ContextOperations {
	public static final String COMMAND_NAMESPACE = "http://ode.apache.org/commands/context";
	public static final String OPERATION_NAMESPACE = "http://ode.apache.org/operations/context";

	@Operation(command = @Command(name = "CreateContext"))
	public static class CreateContext {
		public void createContext() {

		}
	}

	@Operation(command = @Command(name = "LinkContext"))
	public static class LinkContext {
		public void linkContext() {

		}
	}

	@Operation(command = @Command(name = "UnLinkContext"))
	public static class UnLinkContext {
		public void unLinkContext() {

		}
	}

	@Operation(command = @Command(name = "DestroyContext"))
	public static class DestroyContext {
		public void destroyContext() {

		}
	}

}
