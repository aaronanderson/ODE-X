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

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.xml.namespace.QName;

import org.apache.ode.spi.event.xml.Event;
import org.apache.ode.spi.exec.junction.Junction.JunctionException;

public interface Exchange {

	void bond() throws ExchangeException;

	void unbond() throws ExchangeException;

	<T> void addToEnvironment(QName name, T value);

	Map<QName, ?> getEnvironment();

	void removeFromEnvironment(QName name);

	void setDefaultTimeout(long time, TimeUnit unit);

	int wait(int... channels) throws ExchangeException;

	int wait(long time, TimeUnit unit, int... channels) throws ExchangeException;

	<E extends Event> void send(E event, int channel) throws ExchangeException;

	<E extends Event> void receive(E event, int channel) throws ExchangeException;

	public static class ExchangeException extends JunctionException {

	}

}
