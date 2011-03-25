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
package org.apache.ode.api;

import java.beans.ConstructorProperties;

import javax.management.MXBean;

@MXBean
public interface Machine {
	
	public static String OBJECTNAME = "org.apache.ode:type=Machine";
	
	public void install();
	public void uninstall();
	public InstanceID start();
	public void stop(InstanceID instance);
	
	
	public static class InstanceID{
		
		@ConstructorProperties({"name"})
		public InstanceID(String name){
			this.name=name;
		}
		
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
		
		
	}

}
