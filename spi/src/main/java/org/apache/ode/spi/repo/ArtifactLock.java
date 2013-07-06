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

import org.apache.ode.spi.repo.ArtifactLock;

/*@NamedQueries({
		@NamedQuery(name="artifactExists", query="select count(a) from ArtifactImpl a where a.qname = :qname and a.type = :type and a.version = :version"),
		@NamedQuery(name="lookupArtifact", query="select a from ArtifactImpl a where a.qname = :qname and a.type = :type and a.version = :version")
})
@Entity
@Table(name = "ARTIFACT",  uniqueConstraints={@UniqueConstraint(name="ARTIFACT_UNIQUENESS", columnNames={"QNAME","CONTENT_TYPE", "VERSION"})})
*/
public class ArtifactLock implements Serializable {

	private static final long serialVersionUID = 1L;
	//@Id
	//@GeneratedValue(strategy = GenerationType.AUTO)
	private String id;
	
	private String artifactId;

	
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
		if (!(object instanceof ArtifactLock)) {
			return false;
		}
		ArtifactLock other = (ArtifactLock) object;
		if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "org.apache.ode.spi.repo.ArtifactLock[id=" + id + "]";
	}

}
