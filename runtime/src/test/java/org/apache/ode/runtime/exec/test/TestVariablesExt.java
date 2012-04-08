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
package org.apache.ode.runtime.exec.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
/*
  None of this works. After six years and multiple maintenance releases
  why doesn't JAXB bind to maps easily? This class remains as a testament to my hours of fustration.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "TestVariables", namespace = "http://ode.apache.org/execution-context-test")
public class TestVariablesExt {
	
	@XmlJavaTypeAdapter(TestAdapter.class)
	//@XmlElement(name = "Variables", namespace = "http://ode.apache.org/execution-context-test")
	TestMap variables = new TestMap();

	public TestMap getVariables() {
		return variables;
	}

	public void setVariables(TestMap variables) {
		this.variables = variables;
		;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class TestVariable {

		@XmlAttribute(name = "name")
		protected String name;
		@XmlAttribute(name = "value")
		protected String value;

	}

	public static class TestMap extends HashMap<String, String> {
	}

	public static class MyMapType {
		@XmlElement(name = "TestVariable", namespace = "http://ode.apache.org/execution-context-test")
		public List<TestVariable> entry = new ArrayList<TestVariable>();

	}

	public static class TestAdapter extends XmlAdapter<MyMapType, TestMap> {
		@Override
		public TestMap unmarshal(MyMapType value) {
			TestMap map = new TestMap();
			for (TestVariable var : value.entry)
				map.put(var.name, var.value);
			return map;
		}

		@Override
		public MyMapType marshal(TestMap map) {
			MyMapType myMapType = new MyMapType();
			for (Map.Entry<String, String> entry : map.entrySet()) {
				TestVariable var = new TestVariable();
				var.name = entry.getKey();
				var.value = entry.getValue();
				myMapType.entry.add(var);
			}
			return myMapType;
		}

	}
	
	public static class TestAdapter2 extends XmlAdapter<TestVariable[], TestMap> {
		@Override
		public TestMap unmarshal(TestVariable[] value) {
			TestMap map = new TestMap();
			for (TestVariable var : value)
				map.put(var.name, var.value);
			return map;
		}

		@Override
		public TestVariable[] marshal(TestMap map) {
			List<TestVariable> list = new ArrayList<TestVariable>();
			for (Map.Entry<String, String> entry : map.entrySet()) {
				TestVariable var = new TestVariable();
				var.name = entry.getKey();
				var.value = entry.getValue();
				list.add(var);
			}
			return (TestVariable[])list.toArray(new TestVariable[list.size()]);
		}

	}
}
