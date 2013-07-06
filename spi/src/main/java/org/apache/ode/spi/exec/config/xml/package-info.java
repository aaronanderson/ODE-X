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
/*
 * Added to specify namespace prefix during unmarshall
 */
@XmlJavaTypeAdapters({ @XmlJavaTypeAdapter(URIAdapter.class),@XmlJavaTypeAdapter(QNameStringAdapter.class) })
@XmlSchema(namespace = Platform.EXEC_CFG_NAMESPACE, xmlns = { @XmlNs(namespaceURI = Platform.EXEC_CFG_NAMESPACE, prefix = "ecfg") }, elementFormDefault = XmlNsForm.QUALIFIED)
package org.apache.ode.spi.exec.config.xml;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;

import org.apache.ode.spi.exec.Platform;
import org.apache.ode.spi.exec.QNameStringAdapter;
import org.apache.ode.spi.exec.URIAdapter;

