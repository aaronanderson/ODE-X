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
package org.apache.ode.runtime.exec.platform;

import java.io.Serializable;
import java.util.Calendar;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.namespace.QName;

import org.apache.ode.spi.exec.Program;

@Entity
@Table(name = "PROGRAM")
public class ProgramImpl implements Program, Serializable {

	@Id
	@Column(name = "ID")
	private String id;

	@Column(name = "STATUS")
	private String status;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "INSTALL_TIME")
	private Calendar installDate;

	@Column(name = "EXEC_CHECKSUM")
	private String checksum;

	@Column(name = "EXEC_QNAME")
	private String qname;

	@Column(name = "EXEC_VERSION")
	private String version;

	@Lob
	@Basic(fetch = FetchType.LAZY)
	@Column(name = "INSTALL_DATA")
	private byte[] installData;

	public void setId(QName id) {
		this.id = id.toString();
	}

	public QName getId() {
		return QName.valueOf(id);
	}

	@Override
	public QName id() {
		return getId();
	}

	public void setStatus(Status status) {
		this.status = status.name();
	}

	public Status getStatus() {
		if (status != null) {
			return Status.valueOf(this.status);
		}
		return Status.STOPPED;
	}

	@Override
	public Status status() {
		return getStatus();
	}

	public void setInstallDate(Calendar installDate) {
		this.installDate = installDate;
	}

	public Calendar getInstallDate() {
		return installDate;
	}

	@Override
	public Calendar installDate() {
		return getInstallDate();
	}


	public void setInstallData(byte[] installData) {
		this.installData = installData;
	}

	public byte[] getInstallData() {
		return installData;
	}

	@Override
	public byte[] installData() {
		return getInstallData();
	}

	public void setCheckSum(String checksum) {
		this.checksum = checksum;
	}

	public String getCheckSum() {
		return checksum;
	}

	@Override
	public String executableCheckSum() {
		return getCheckSum();
	}

	public void setQName(QName qname) {
		this.qname = qname.toString();
	}

	public QName getQName() {
		return QName.valueOf(qname);
	}
	
	@Override
	public QName executableQName() {
		return getQName();
	}

	
	public void setVersion(String version) {
		this.version = version;
	}

	public String getVersion() {
		return version;
	}
	
	@Override
	public String executableVersion() {
		return getVersion();
	}

	@Override
	public List<String> nodes() {
		// TODO Auto-generated method stub
		return null;
	}

}
