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

import java.net.URI;
import java.util.UUID;

import org.apache.ode.spi.exec.config.xml.BondId;

public interface Compound {
	//all compounds implicitly are associated with with base Execution

	UUID id();

	<B extends Bond> B getBondPoint(BondId id);

	<B extends Bond> B getBondPoint(URI relativeUri); //unique in Formula

	//explicity attach to existing Compond
	void associate(UUID compound);

	void disassociate(UUID compound);

	void attach(); //attachs the current Compound to Java thread so that bond points are contextually aware

	void detach();

}