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
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.FetchType;

import org.apache.ode.spi.exec.ActionTask.ActionStatus;
import org.apache.ode.spi.exec.MasterActionTask.MasterActionStatus;

@Entity
@DiscriminatorValue("MASTER")
public class MasterAction extends Action implements MasterActionStatus, Serializable {

	@OneToMany(mappedBy="master",cascade = CascadeType.ALL, fetch=FetchType.LAZY)
	@JoinTable(name = "ACTION_SLAVES", joinColumns = { @JoinColumn(name = "MASTER_ID", referencedColumnName="ACTION_ID") }, inverseJoinColumns = { @JoinColumn(name = "SLAVE_ID", referencedColumnName="ACTION_ID") })
	Set<SlaveAction> slaves;

	@Override
	public Set<ActionStatus> slaveStatus() {
		Set<ActionStatus> stats = new HashSet<ActionStatus>();
		if (slaves != null) {
			stats.addAll(slaves);
		}
		return stats;
	}

	public Set<SlaveAction> getSlaves() {
		return slaves;
	}

	public void addSlave(SlaveAction slave) {
		if (slaves == null) {
			slaves = new HashSet<SlaveAction>();
		}
		slaves.add(slave);
	}

}
