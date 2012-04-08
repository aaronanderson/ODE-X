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

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;

import org.apache.ode.spi.exec.ActionTask.ActionStatus;
import org.apache.ode.spi.exec.SlaveActionTask.SlaveActionStatus;

@Entity
@DiscriminatorValue("SLAVE")
public class SlaveAction extends Action implements SlaveActionStatus, Serializable {

	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.REFRESH })
	@JoinTable(name = "ACTION_SLAVES", joinColumns = { @JoinColumn(name = "SLAVE_ID", referencedColumnName="ACTION_ID") }, inverseJoinColumns = { @JoinColumn(name = "MASTER_ID", referencedColumnName="ACTION_ID") })
	MasterAction master;

	@Override
	public ActionStatus masterStatus() {
		return master;
	}

	public void setMaster(MasterAction master) {
		this.master = master;
	}

}
