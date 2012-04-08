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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;

public class AnnotatedFieldImpl<T> extends AnnotatedImpl implements AnnotatedField<T> {

	AnnotatedType<? super T> declaringType;
	boolean isStatic;
	Field javaMember;

	public AnnotatedFieldImpl(Type baseType, Set<Type> typeClosure, Set<Annotation> annotations,
			AnnotatedType<? super T> annotatedType, boolean isStatic, Field javaMember) {
		super(baseType, typeClosure, annotations);
		this.declaringType = annotatedType;
		this.isStatic = isStatic;
		this.javaMember = javaMember;
	}

	public AnnotatedFieldImpl(AnnotatedField<? super T> field) {
		this(field.getBaseType(), field.getTypeClosure(), field.getAnnotations(), field.getDeclaringType(),
				field.isStatic(), field.getJavaMember());
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
	public Field getJavaMember() {
		return javaMember;
	}

}
