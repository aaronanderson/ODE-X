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
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

public class AnnotatedTypeImpl<T> extends AnnotatedImpl implements
		AnnotatedType<T> {

	Set<AnnotatedConstructor<T>> constructors;
	Set<AnnotatedField<? super T>> fields;
	Class<T> javaClass;
	Set<AnnotatedMethod<? super T>> methods;

	public AnnotatedTypeImpl(Type baseType, Set<Type> typeClosure,
			Set<Annotation> annotations,
			Set<AnnotatedConstructor<T>> constructors,
			Set<AnnotatedField<? super T>> fields, Class<T> javaClass,
			Set<AnnotatedMethod<? super T>> methods) {
		super(baseType, typeClosure, annotations);
		this.constructors = new HashSet<AnnotatedConstructor<T>>(constructors);
		this.fields = new HashSet<AnnotatedField<? super T>>();
		for (AnnotatedField<? super T> field: fields){
			this.fields.add(new AnnotatedFieldImpl(field));
		}
		this.javaClass = javaClass;
		this.methods = new HashSet<AnnotatedMethod<? super T>>();
		for (AnnotatedMethod<? super T> method: methods){
			this.methods.add(new AnnotatedMethodImpl(method));
		}
	}

	public AnnotatedTypeImpl(AnnotatedType<T> type) {
		this(type.getBaseType(), type.getTypeClosure(), type.getAnnotations(),
				type.getConstructors(), type.getFields(), type.getJavaClass(),
				type.getMethods());
	}

	public Set<AnnotatedConstructor<T>> getConstructors() {
		return constructors;
	}

	public void setConstructors(Set<AnnotatedConstructor<T>> constructors) {
		this.constructors = constructors;
	}

	public Set<AnnotatedField<? super T>> getFields() {
		return fields;
	}

	public Class<T> getJavaClass() {
		return javaClass;
	}

	public Set<AnnotatedMethod<? super T>> getMethods() {
		return methods;
	}

}