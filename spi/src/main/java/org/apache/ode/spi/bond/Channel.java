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
package org.apache.ode.spi.bond;

import java.util.concurrent.TimeUnit;


public interface Channel<E> {

	Mode mode();

	void setDefaultTimeout(long time, TimeUnit unit);

	boolean send(E event) throws ChannelException;

	boolean send(E event, long time, TimeUnit unit) throws ChannelException;

	E receive() throws ChannelException;

	E receive(long time, TimeUnit unit);

	public static class ChannelException extends BondException {

	}

	public static enum Mode {
		IN, OUT, IN_OUT;
	}

}