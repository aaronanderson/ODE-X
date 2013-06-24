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
package org.apache.ode.spi.exec.junction;

import java.util.concurrent.TimeUnit;

import org.apache.ode.spi.exec.junction.ProgramNode.ProgramException;

public interface Junction {

	void setDefaultTimeout(long time, TimeUnit unit);

	Exchange push() throws JunctionException;

	Exchange push(long time, TimeUnit unit)throws JunctionException;

	Exchange pull()throws JunctionException;

	Exchange pull(long time, TimeUnit unit);

	<T> void bind(T junction);

	<T> void unbind(T junction);
	
	public static class JunctionException extends ProgramException {

	}

}
