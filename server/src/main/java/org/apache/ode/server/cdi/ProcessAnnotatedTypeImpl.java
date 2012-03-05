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
package org.apache.ode.server.cdi;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.ProcessAnnotatedType;

public class ProcessAnnotatedTypeImpl<T> implements ProcessAnnotatedType<T> {

	AnnotatedType<T> type;
	boolean veto = false;

	public ProcessAnnotatedTypeImpl(AnnotatedType<T> type) {
		this.type = type;
	}

	public boolean isVeto() {
		return veto;
	}

	@Override
	public AnnotatedType<T> getAnnotatedType() {
		return type;
	}

	@Override
	public void setAnnotatedType(AnnotatedType<T> type) {
		this.type = type;

	}

	@Override
	public void veto() {
		veto = true;

	}

	public void fire(BeanManager bm) {
		bm.fireEvent(new ParameterizedTypeImpl(ProcessAnnotatedType.class, new Type[] { type.getBaseType() }));
	}
	
	public static class ParameterizedTypeImpl implements ParameterizedType{
		
		Type rawType;
		Type[] actualTypeArguments;
		
		public ParameterizedTypeImpl(Type rawType, Type[] actualTypeArguments){
			this.rawType=rawType;
			this.actualTypeArguments=actualTypeArguments;
		}
		
		@Override
		public Type[] getActualTypeArguments() {
			return actualTypeArguments;
		}

		@Override
		public Type getRawType() {
			return rawType;
		}

		@Override
		public Type getOwnerType() {
			return null;
		}
	}
}
