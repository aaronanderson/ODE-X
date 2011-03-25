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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;

public class AnnotatedMethodImpl<T> extends AnnotatedImpl implements AnnotatedMethod<T> {

	List<AnnotatedParameter<T>> parameters;
	AnnotatedType<? super T> declaringType;
	boolean isStatic;
	Method javaMember;

	public AnnotatedMethodImpl(Type baseType, Set<Type> typeClosure, Set<Annotation> annotations, List<?> parameters,
			AnnotatedType<? super T> declaringType, boolean isStatic, Method javaMember) {
		super(baseType, typeClosure, annotations);
		this.parameters = new ArrayList<AnnotatedParameter<T>>();
		for (AnnotatedParameter<T> param : (List<AnnotatedParameter<T>>)parameters) {
			this.parameters.add(new AnnotatedParameterImpl(param));
		}
		this.declaringType = declaringType;
		this.isStatic = isStatic;
		this.javaMember = javaMember;
	}

	public AnnotatedMethodImpl(AnnotatedMethod<? super T> method) {
		this(method.getBaseType(), method.getTypeClosure(), method.getAnnotations(), method.getParameters(), method
				.getDeclaringType(), method.isStatic(), method.getJavaMember());
	}

	@Override
	public List<AnnotatedParameter<T>> getParameters() {
		return parameters;
	}

	@Override
	public AnnotatedType<T> getDeclaringType() {
		return (AnnotatedType<T>) declaringType;
	}

	@Override
	public boolean isStatic() {
		return isStatic;
	}

	@Override
	public Method getJavaMember() {
		return javaMember;
	}

}
