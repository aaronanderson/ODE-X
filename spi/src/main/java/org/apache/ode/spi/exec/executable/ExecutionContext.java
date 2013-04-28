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
package org.apache.ode.spi.exec.executable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import org.apache.ode.spi.exec.ThreadScope;

import static org.apache.ode.spi.exec.Platform.EXEC_CTX_NAMESPACE;


//Model and default view of ExecutionState (MVC and Observer patterns)
@ThreadScope
public class ExecutionContext {
	
	public static final QName CALL_FRAME = new QName(EXEC_CTX_NAMESPACE, "CallFrame");
	public static final QName VARIABLE_FRAME = new QName(EXEC_CTX_NAMESPACE, "VariableFrame");
	
	public static enum ChangeType{
		ADD,UPDATE,REMOVE, ALL;
	}
	
	@Target({ ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface ChangeListener {
		public String namespace();
		public String name();
		public ChangeType type() default ChangeType.ALL;		

	}
	
	
	
	//examples
	//@ChangeListener(namespace=EXEC_CTX_NS,name=MY_STACK_ATTRIBUTE)
	//public void myStackAttributeChange(ChangeType type, MyStack parent, MyStackAttribute value){
	
	//@ChangeListener(namespace=EXEC_CTX_NS,name=MY_STACK_ATTRIBUTE,type=ChangeType.ADD)
	//public void myStackAttributeAdd(MyStack parent, MyStackAttribute value){
		
	
	/*protected ExecutionState execState;
	
	public ExecutionContext(ExecutionState execState){
		this.execState=execState;
	}*/
	
	public void pushCall(){
		
	}
	
	public void popCall(){
		
	}
	
	public void registerChange(QName name, Object listener){
		
		
	}
	
	
	public void registerChangeListener(QName name, Object listener){
	
	
	}
		

	public void fireChange(QName name, ChangeType type, Object parent, Object value){
		
	}

	@PostConstruct
	public void init(){
		
	}
	
	//public void declare
}