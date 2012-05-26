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
package org.apache.ode.runtime.exec.platform;

import java.io.Serializable;
import java.util.logging.Logger;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import org.apache.ode.spi.exec.Target;

//@NamedQueries({ @NamedQuery(name = "localTasks", query = "select action from Action action where action.nodeId = :nodeId and action.state = 'SUBMIT'  or ( action.state = 'CANCELED' and action.finish is null )") })
@Entity
@Table(name = "TARGET")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "TYPE", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("TARGET")
public class TargetImpl implements Target, Serializable {

	private static final long serialVersionUID = 4646331556426734662L;

	private static final Logger log = Logger.getLogger(TargetImpl.class.getName());

	public long getTargetId() {
		return targetId;
	}

	public void setTargetId(long targetId) {
		this.targetId = targetId;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Id
	@GeneratedValue
	@Column(name = "TARGET_ID")
	protected long targetId;

	@Column(name = "ID")
	protected String id;

	@Entity
	@DiscriminatorValue("ALL")
	public static class TargetAllImpl extends TargetImpl implements TargetAll, Serializable {
	}

	@Entity
	@DiscriminatorValue("CLUSTER")
	public static class TargetClusterImpl extends TargetImpl implements TargetCluster, Serializable {

		@Override
		public String clusterId() {
			return id;
		}

	}

	@Entity
	@DiscriminatorValue("NODE")
	public static class TargetNodeImpl extends TargetImpl implements TargetNode, Serializable {
		
		public TargetNodeImpl(){}
		
		public TargetNodeImpl(String nodeId){
			this.id=nodeId;
		}
		@Override
		public String nodeId() {
			return id;
		}
	}
}
