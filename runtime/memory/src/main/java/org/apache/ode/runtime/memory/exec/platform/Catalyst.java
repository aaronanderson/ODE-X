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
package org.apache.ode.runtime.memory.exec.platform;

import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.ode.spi.exec.platform.Dispatch.Token;
//TODO consider using ForkJoin. Did not take this approach due to ForkJoin being excluded from JSR 236, would like more control over 
//execution performance (limited with ForkJoin), and would like ability to run in single thread.

public class Catalyst implements Runnable {
	
	public static final String CATALYST_NAMESPACE = "http://ode.apache.org/runtime/memory/catalyst";
	
	public static enum State {
		BLOCK, RUN, COMPLETE;
	}

	PriorityBlockingQueue<Catalyst> executionQueue;
	Queue<? extends Token> tokens;
	AtomicReference<State> state;

	@Override
	public void run() {
		//check sub executions
		if (executionQueue != null && !executionQueue.isEmpty()) {

		}
		do {
			if (tokens == null || tokens.isEmpty()) {
				state.set(State.COMPLETE);
				continue;
			}
			Token t = tokens.peek();
			switch (t.type()) {
			case ABORT:
				break;
			case BEGIN_PARALLEL:
				break;
			case BEGIN_SEQUENCE:
				break;
			case BLOCK:
				break;
			case COMMAND:
				break;
			case END_PARALLEL:
				break;
			case END_SEQUENCE:
				break;
			case ENVIRONMENT:
				break;
			case OPERATION:
				break;
			case PIPE:
				break;
			default:
				break;

			}

		} while (State.RUN.equals(state.get()));
	}

}
