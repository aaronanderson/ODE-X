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
package org.apache.ode.runtime.ws;

import javax.inject.Inject;

import org.apache.ode.spi.event.Channel;
import org.apache.ode.spi.event.Publisher;
import org.apache.ode.spi.event.Subscriber;

public class JAXWSEventProcessor {
	public static final String REQUEST="{http://ode.apache.org/runtime/event}Request";
	public static final String RESPONSE="{http://ode.apache.org/runtime/event}Response";
	
	@Inject
	@Publisher(REQUEST)
	Channel<RequestEvent> channel;

	public void handleRequest() {

	}

	public void handleResponse(@Subscriber(RESPONSE) ResponseEvent response) {

	}

}
