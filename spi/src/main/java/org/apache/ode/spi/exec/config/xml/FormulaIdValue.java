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
package org.apache.ode.spi.exec.config.xml;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class FormulaIdValue implements FormulaId {

	public FormulaIdValue(String id) {
		this.id = id;
	}

	private String id;

	@Override
	public String id() {
		return id;
	}
	
	
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FormulaIdValue other = (FormulaIdValue) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}




	public static class FormulaIdAdapter extends XmlAdapter<String, FormulaId> {

		@Override
		public String marshal(FormulaId id) throws Exception {
			if (id != null) {
				return id.id();
			}
			return null;
		}

		@Override
		public FormulaId unmarshal(String id) throws Exception {
			FormulaIdValue i = new FormulaIdValue(id);
			return i;
		}

	}

}
