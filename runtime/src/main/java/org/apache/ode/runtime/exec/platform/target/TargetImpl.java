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
package org.apache.ode.runtime.exec.platform.target;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.apache.ode.runtime.exec.platform.NodeStatusImpl;
import org.apache.ode.runtime.exec.platform.target.TargetImpl.TargetPK;
import org.apache.ode.spi.exec.target.Target;

//@NamedQueries({ @NamedQuery(name = "localTasks", query = "select action from Action action where action.nodeId = :nodeId and action.state = 'SUBMIT'  or ( action.state = 'CANCELED' and action.finish is null )") })
@Entity
@Table(name = "TARGET")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "TYPE", discriminatorType = DiscriminatorType.STRING)
@IdClass(TargetPK.class)
public abstract class TargetImpl implements Target, Serializable {

	private static final long serialVersionUID = 4646331556426734662L;

	private static final Logger log = Logger.getLogger(TargetImpl.class.getName());

	@Id
	@Column(name = "ID")
	protected String id;

	@Id
	@Column(name = "TYPE")
	protected String type;

	@OneToMany
	@JoinTable(name = "ODE_TARGET_NODES", joinColumns = { @JoinColumn(name = "TARGET_ID", referencedColumnName = "ID"),
			@JoinColumn(name = "TARGET_TYPE", referencedColumnName = "TYPE") }, inverseJoinColumns = { @JoinColumn(name = "NODE_ID", referencedColumnName = "NODE_ID") })
	protected Set<NodeStatusImpl> nodes;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Set<NodeStatusImpl> getNodes() {
		return nodes;
	}

	public void setNodes(Set<NodeStatusImpl> nodes) {
		this.nodes = nodes;
	}

	@Override
	public String id() {
		return id;
	}

	@Override
	public String type() {
		return type;
	}

	@Override
	public String[] nodeIds() {
		List<String> ids = new ArrayList<String>();
		for (NodeStatusImpl impl : nodes) {
			//if (NodeState.ONLINE == impl.state()) {
			ids.add(impl.nodeId());
			//}
		}
		return ids.toArray(new String[ids.size()]);
	}

	public static class TargetPK {
		public String id;
		public String type;

		public TargetPK() {
		}

		public TargetPK(String id, String type) {
			this.id = id;
			this.type = type;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((id == null) ? 0 : id.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TargetPK other = (TargetPK) obj;
			if (id == null) {
				if (other.id != null)
					return false;
			} else if (!id.equals(other.id))
				return false;
			if (type == null) {
				if (other.type != null)
					return false;
			} else if (!type.equals(other.type))
				return false;
			return true;
		}

	}
}
