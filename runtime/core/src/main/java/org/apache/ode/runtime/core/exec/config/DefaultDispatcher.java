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
package org.apache.ode.runtime.core.exec.config;

import java.lang.invoke.MethodHandle;
import java.util.Map;
import java.util.Queue;

import javax.xml.namespace.QName;

import org.apache.ode.spi.work.Dispatch;
import org.apache.ode.spi.work.Dispatch.CommandMap;
import org.apache.ode.spi.work.Dispatch.Dispatcher;
import org.apache.ode.spi.work.Dispatch.OperationMap;
import org.apache.ode.spi.work.Dispatch.Token;

@Dispatcher
public class DefaultDispatcher {

	@Dispatch()
	public void dispatch(Queue<? extends Token> dispatchQueue, @CommandMap Map<QName, MethodHandle>  commands, @OperationMap Map<QName, MethodHandle>  operations) {

	}

}
