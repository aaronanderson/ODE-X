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
package org.apache.ode.server.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedCallable;
import javax.enterprise.inject.spi.AnnotatedParameter;

public class AnnotatedParameterImpl<T> extends AnnotatedImpl implements
		AnnotatedParameter<T> {
	AnnotatedCallable<T> callable;
	int position;
	
	public AnnotatedParameterImpl(Type baseType, Set<Type> typeClosure,
			Set<Annotation> annotations, AnnotatedCallable<T> callable, int position) {
		super(baseType, typeClosure, annotations);
		this.callable= callable;
		this.position = position;
	}

	public AnnotatedParameterImpl(AnnotatedParameter<T> param) {
		this(param.getBaseType(), param.getTypeClosure(), param.getAnnotations(),param.getDeclaringCallable(),param.getPosition());
	}

	@Override
	public AnnotatedCallable<T> getDeclaringCallable() {
		return callable;
	}

	@Override
	public int getPosition() {
		return position;
	}

}
