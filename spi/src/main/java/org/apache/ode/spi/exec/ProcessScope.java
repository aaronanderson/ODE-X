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
package org.apache.ode.spi.exec;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Scope;

/**
 *	Any object in this scope is bound to the ODE process context. If the annotated class has the named annotation then it can
 *be shared in the scope, otherwise no instances are shared. This scope is useful for sharing data across instruction and thread invocations. Objects
 *stored in this scope should be thread safe as the runtime will concurrently execute multiple Java threads with the same scope instance.
 */
@Scope
@Retention(RUNTIME)
@Target(TYPE)
public @interface ProcessScope {
	boolean singleton() default false;
}
