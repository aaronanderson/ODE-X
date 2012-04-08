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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.Annotated;

public class AnnotatedImpl implements Annotated {

	Type baseType;
	Set<Type> typeClosure;
	Set<Annotation> annotations;

	public AnnotatedImpl(Type baseType, Set<Type> typeClosure,
			Set<Annotation> annotations) {
		this.baseType = baseType;
		this.typeClosure = new HashSet<Type>(typeClosure);
		this.annotations = new HashSet<Annotation>(annotations);
	}

	public <T extends Annotation> T getAnnotation(
			java.lang.Class<T> annotationType) {
		for (Annotation a : annotations) {
			if (annotationType.isInstance(a)) {
				return annotationType.cast(a);
			}
		}
		return null;
	}

	public Set<Annotation> getAnnotations() {
		return annotations;
	}

	public Type getBaseType() {
		return baseType;
	}

	public Set<Type> getTypeClosure() {
		return typeClosure;
	}

	public boolean isAnnotationPresent(
			Class<? extends Annotation> annotationType) {
		return getAnnotation(annotationType) != null;
	}
}
