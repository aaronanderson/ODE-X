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
package org.apache.ode.runtime.build;

import javax.xml.namespace.QName;

import org.apache.ode.spi.compiler.Source;

public class SourceImpl implements Source {

	QName qname;
	String contentType;
	String version;
	String checkSum;
	byte[] contents;
	SourceType sourceType;

	public SourceImpl(QName qname, String contentType, String version, String checkSum, byte[] contents, SourceType sourceType) {
		this.qname = qname;
		this.contentType = contentType;
		this.version = version;
		this.checkSum = checkSum;
		this.contents = contents;
		this.sourceType = sourceType;
	}

	@Override
	public QName getQName() {
		return qname;
	}

	@Override
	public String getContentType() {
		return contentType;
	}

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public String getCheckSum() {
		return checkSum;
	}

	@Override
	public byte[] getContent() {
		return contents;
	}

	@Override
	public SourceType sourceType() {
		return sourceType;
	}

	@Override
	public String toString() {
		return String.format("QName: %s ContentType: %s Version: %s", qname, contentType, version);
	}
}
