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
package org.ode.data.memory.repo;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;

import org.apache.ode.data.memory.repo.xml.Entry;
import org.apache.ode.data.memory.repo.xml.Repository;
import org.apache.ode.spi.repo.Artifact;

public class FileRepository extends Repository {

	ReadWriteLock lock = new ReentrantReadWriteLock();
	boolean synced = false;
	boolean updateable = false;
	URI location;

	public ReadWriteLock getLock() {
		return lock;
	}

	public boolean isSynced() {
		return synced;
	}

	public boolean isUpdateable() {
		return updateable;
	}

	public void setUpdateable(boolean updateable) {
		this.updateable = updateable;
	}

	public URI getLocation() {
		return location;
	}

	public void setLocation(URI location) {
		this.location = location;
	}

	public static class LocalArtifact {
		URI location;
		Artifact artifact;

		public URI getLocation() {
			return location;
		}

		public void setLocation(URI location) {
			this.location = location;
		}

		public Artifact getArtifact() {
			return artifact;
		}

		public void setArtifact(Artifact artifact) {
			this.artifact = artifact;
		}

	}
	
	@XmlJavaTypeAdapter(EntryMapAdapter.class)
	public static class EntryMap extends LinkedHashMap<EntryKey, LocalArtifact> {

	}

	public class EntryMapAdapter extends XmlAdapter<List<Entry>, EntryMap> {

		@Override
		public EntryMap unmarshal(List<Entry> entries) throws Exception {
			EntryMap map = new EntryMap();
			for (Entry entry : entries) {
				Artifact artifact = new Artifact();
				artifact.setQName(QName.valueOf(entry.getName()));
				artifact.setContentType(entry.getContentType());
				artifact.setVersion(entry.getVersion());
				LocalArtifact lartifact = new LocalArtifact();
				lartifact.setArtifact(artifact);
				map.put(new EntryKey(artifact), lartifact);
			}
			return map;
		}

		@Override
		public List<Entry> marshal(EntryMap map) throws Exception {

			List<Entry> entries = new ArrayList<Entry>();
			for (LocalArtifact lartifact : map.values()) {
				Entry entry = new Entry();
				entry.setName(lartifact.getArtifact().getQName().toString());
				entry.setContentType(lartifact.getArtifact().getContentType());
				entry.setVersion(lartifact.getArtifact().getVersion());
				entry.setLocation(lartifact.getLocation().toString());
				entries.add(entry);
			}
			return entries;
		}
	}

}
