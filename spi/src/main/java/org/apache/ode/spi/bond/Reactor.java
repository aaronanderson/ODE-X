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

import java.util.UUID;

import org.apache.ode.spi.exec.config.xml.Formula;
import org.apache.ode.spi.exec.config.xml.FormulaId;

public interface Reactor {
	//all compounds  are implicitly associated with with base Execution

	void addFormula(Formula formula);

	void listFormulas();
	
	void getFormula(FormulaId id);
	
 	void modifyFormula(Formula formula);

	void removeFormula(Formula formula);

	<C extends Compound> C addCompound(FormulaId id);

	<C extends Compound> C getCompound(UUID id);

	<C extends Compound> C removeCompound(FormulaId id);

	

}
