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
package org.apache.ode.runtime.build;

import javax.xml.namespace.QName;

import org.apache.ode.spi.compiler.InlineSource;
import org.apache.ode.spi.compiler.Location;
import org.apache.ode.spi.compiler.Source;
import org.apache.ode.spi.exec.executable.xml.SourceId;

public class SourceImpl implements Source {

	QName qname;
	String contentType;
	String version;
	String checkSum;
	byte[] contents;
	SourceType sourceType;
	org.apache.ode.spi.exec.executable.xml.Source xmlSrc;

	public SourceImpl(QName qname, String contentType, String version, String checkSum, byte[] contents, String id, SourceType sourceType) {
		this.qname = qname;
		this.contentType = contentType;
		this.version = version;
		this.checkSum = checkSum;
		this.contents = contents;
		this.xmlSrc= new org.apache.ode.spi.exec.executable.xml.Source();
		this.xmlSrc.setSrc(new SourceId(id));
		this.xmlSrc.setQname(qname);
		this.xmlSrc.setContentType(contentType);
		this.xmlSrc.setVersion(version);
		this.sourceType = sourceType;
	}
	
	public SourceImpl(SourceImpl impl) {
		this.qname = impl.getQName();
		this.contentType = impl.getContentType();
		this.version = impl.getVersion();
		this.checkSum = impl.getCheckSum();
		this.contents = impl.getContent();
		this.xmlSrc= impl.xmlSrc();
		this.sourceType = impl.sourceType();
	}
	
	org.apache.ode.spi.exec.executable.xml.Source xmlSrc(){
		return xmlSrc;
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
	public org.apache.ode.spi.exec.executable.xml.Source srcRef() {
		return xmlSrc;
	}

	@Override
	public SourceType sourceType() {
		return sourceType;
	}

	@Override
	public String toString() {
		return String.format("QName: %s ContentType: %s Version: %s", qname, contentType, version);
	}

	public static class InlineSourceImpl extends SourceImpl implements InlineSource {

		String inlineContentType;
		Location start;
		Location end;

		public InlineSourceImpl(SourceImpl parent, String inlineCT, Location start, Location end) {
			super(parent);
			this.inlineContentType = inlineCT;
			this.start = start;
			this.end = end;
		}

		@Override
		public String inlineContentType() {
			return inlineContentType;
		}

		@Override
		public Location start() {
			return start;
		}

		@Override
		public Location end() {
			return end;
		}

	}
}
