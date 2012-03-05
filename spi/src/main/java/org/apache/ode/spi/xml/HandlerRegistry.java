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
package org.apache.ode.spi.xml;

import javax.xml.namespace.QName;

public interface HandlerRegistry<M, C, X extends HandlerException, E extends ElementHandler<? extends M, C, X>, A extends AttributeHandler<? extends M, C, X>> {

	public void register(E handler, QName... ename) throws X;

	public void unregister(QName ename, Class<E> type) throws X;

	public E retrieve(QName ename, M model) throws X;

	public void register(A handler, QName aname, QName... ename) throws X;

	public void unregister(QName ename, QName aname, Class<A> type) throws X;

	public A retrieve(QName ename, QName aname, M model) throws X;

}
