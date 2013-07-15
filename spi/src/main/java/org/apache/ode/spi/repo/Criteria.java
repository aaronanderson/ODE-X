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

import java.net.URI;
import java.util.UUID;

public class Criteria {

	private UUID id;

	private URI uri;

	private URI collection;

	private String uriPattern;

	private String type;

	private String version;

	public Criteria() {

	}

	public Criteria(UUID id) {
		this.id = id;
	}

	public Criteria(URI uri) {
		this.uri = uri;
	}

	public Criteria(URI uri, String contentType) {
		this(uri);
		this.type = contentType;
	}

	public Criteria(URI uri, String version, String contentType) {
		this(uri, contentType);
		this.version = version;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public Criteria withId(UUID id) {
		this.id = id;
		return this;
	}

	public UUID getId() {
		return id;
	}

	public void setURI(URI uri) {
		this.uri = uri;
	}

	public Criteria withURI(URI uri) {
		this.uri = uri;
		return this;
	}

	public URI getURI() {
		return uri;
	}

	public void setCollection(URI collection) {
		this.collection = collection;
	}

	public Criteria withCollection(URI collection) {
		this.collection = collection;
		return this;
	}

	public URI getCollection() {
		return collection;
	}

	public void setURIPattern(String uriPattern) {
		this.uriPattern = uriPattern;
	}

	public Criteria withURIPattern(String uriPattern) {
		this.uriPattern = uriPattern;
		return this;
	}

	public String getURIPattern() {
		return uriPattern;
	}

	public void setContentType(String type) {
		this.type = type;
	}

	public Criteria withContentType(String type) {
		this.type = type;
		return this;
	}

	public String getContentType() {
		return type;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public Criteria withVersion(String version) {
		this.version = version;
		return this;
	}

	public String getVersion() {
		return version;
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
		if (!(object instanceof Criteria)) {
			return false;
		}
		Criteria other = (Criteria) object;
		if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "org.apache.ode.spi.repo.Criteria[id=" + id + "]";
	}

}
