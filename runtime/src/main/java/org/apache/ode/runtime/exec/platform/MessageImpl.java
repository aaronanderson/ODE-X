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
import java.util.Date;
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
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.ode.spi.exec.Message;
import org.apache.ode.spi.exec.Message.LogLevel;
import org.apache.ode.spi.exec.Target.TargetAll;
import org.apache.ode.spi.exec.Target.TargetCluster;
import org.apache.ode.spi.exec.Target.TargetNode;

//@NamedQueries({ @NamedQuery(name = "localTasks", query = "select action from Action action where action.nodeId = :nodeId and action.state = 'SUBMIT'  or ( action.state = 'CANCELED' and action.finish is null )") })
@Entity
@Table(name = "MESSAGE")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "TYPE", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("MESSAGE")
public class MessageImpl implements Message, Serializable {

	private static final long serialVersionUID = 4646331556426734662L;

	private static final Logger log = Logger.getLogger(MessageImpl.class.getName());
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "timestamp")
	private Date timestamp;
	
	@Column(name = "LEVEL")
	private String level;
	
	@Column(name = "CODE")
	private int code;
	
	@Column(name = "MESSAGE")
	private String message;

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	
	public Date timestamp(){
		return timestamp;
	}

	public void setLevel(String level) {
		this.level = level;
	}
	public LogLevel level(){
		return LogLevel.valueOf(level);
	}

	public void setCode(int code) {
		this.code = code;
	}
	
	public int code(){
		return code;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
	public String message(){
		return message;
	}
	
	
	
}
