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
package org.apache.ode.spi.exec.executable;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(TYPE)
public @interface Instruction {

	@Retention(RUNTIME)
	@Target(METHOD)
	public @interface Execute {
		

	}
	
	/**
	 annotate method of Endpoint implementation class to be invoked when an event is queued in an event destination address 
	 */
	@Target({ ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface EventExecute {
		//must contain property field name corresponding to event destination address
		public String value();

	}
	
	//Oridinary
	//@Execute
	//void execute(ExecutionContext ectx);

	
	//Exchange
	//@Execute
	//void execute(EventBus bus);
	
	//DestinationStreamAdd myEventAdd
	
	//@EventExecute("myEventAdd")
	//void handleMyEvent(MyEvent event)
	
	
	
	//need facade classes to 
	//      a) simplify interactions with execution context xml state
	//      b) reuse language specific context operations (define/set/get variables in lang specific way)
	//void execute(MyFrame frame);
	//void execute(MyStack stack);
	//MyFrame
	//  declareMyHandler
	// raiseMyException
	// declareMyVariable
	
	//blockMyExchange
	//
	
	//TODO should state be separate from context?

}