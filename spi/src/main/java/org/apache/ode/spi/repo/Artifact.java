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
package org.apache.ode.spi.repo;

import java.io.Serializable;

import javax.xml.namespace.QName;

import org.apache.ode.spi.repo.Artifact;

/*@NamedQueries({
		@NamedQuery(name="artifactExists", query="select count(a) from ArtifactImpl a where a.qname = :qname and a.type = :type and a.version = :version"),
		@NamedQuery(name="lookupArtifact", query="select a from ArtifactImpl a where a.qname = :qname and a.type = :type and a.version = :version")
})
@Entity
@Table(name = "ARTIFACT",  uniqueConstraints={@UniqueConstraint(name="ARTIFACT_UNIQUENESS", columnNames={"QNAME","CONTENT_TYPE", "VERSION"})})
*/
public class Artifact implements Serializable {

	private static final long serialVersionUID = 1L;
	//@Id
	//@GeneratedValue(strategy = GenerationType.AUTO)
	private String id;

	//@Column(name = "QNAME")
	private String qname;

	//@Column(name = "CONTENT_TYPE")
	private String type;

	//@Column(name = "VERSION")
	private String version;

	//@Column(name = "CHECKSUM")
	private String checksum;

	/*
	 * @Column(name = "INDEX_ATTR1") private String indexAttr1;
	 */

	// @Column(name = "CONTENT_LENGTH")
	// private long contentLength;

	//@Column(name = "CONTENT")
	//@Lob
	//@Basic(fetch = FetchType.LAZY)
	private byte[] content;

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setQName(QName qname) {
		this.qname = qname.toString();
	}

	public QName getQName() {
		return QName.valueOf(qname);
	}

	public void setContentType(String type) {
		this.type = type;
	}

	public String getContentType() {
		return type;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getVersion() {
		return version;
	}

	public String getCheckSum() {
		return checksum;
	}

	public byte[] getContent() {
		return content;
	}

	public void setContent(byte[] content) {
		this.content = content;
	}

	@Override
	public int hashCode() {
		int hash = 0;
		hash += (id != null ? id.hashCode() : 0);
		return hash;
	}

	@Override
	public boolean equals(Object object) {
		// TODO: Warning - this method won't work in the case the id fields are
		// not set
		if (!(object instanceof Artifact)) {
			return false;
		}
		Artifact other = (Artifact) object;
		if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "org.apache.ode.spi.repo.Artifact[id=" + id + "]";
	}

}
