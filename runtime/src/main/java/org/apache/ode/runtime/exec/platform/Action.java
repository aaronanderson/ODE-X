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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.ode.runtime.exec.cluster.xml.LogType;
import org.apache.ode.runtime.exec.cluster.xml.Message;
import org.apache.ode.runtime.exec.cluster.xml.Messages;
import org.apache.ode.runtime.exec.cluster.xml.ObjectFactory;
import org.apache.ode.spi.exec.ActionTask.ActionId;
import org.apache.ode.spi.exec.ActionTask.ActionMessage;
import org.apache.ode.spi.exec.ActionTask.ActionState;
import org.apache.ode.spi.exec.ActionTask.ActionStatus;
import org.apache.ode.spi.exec.PlatformException;
import org.w3c.dom.Document;

@NamedQueries({ @NamedQuery(name = "localTasks", query = "select action from Action action where action.nodeId = :nodeId and action.state = 'SUBMIT'  or ( action.state = 'CANCELED' and action.finish is null )") })
@Entity
@Table(name = "ACTION")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "TYPE", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("ACTION")
public class Action implements ActionStatus, Serializable {

	public static DocumentBuilderFactory domFactory;
	public static TransformerFactory transformFactory;
	static {
		domFactory = DocumentBuilderFactory.newInstance();
		domFactory.setNamespaceAware(true);
		transformFactory = TransformerFactory.newInstance();
	}

	@Id
	@GeneratedValue
	@Column(name = "ACTION_ID")
	private long actionId;

	@Column(name = "NODE_ID")
	private String nodeId;

	@Column(name = "NAME")
	private String name;

	@Column(name = "COMPONENT")
	private String component;

	@Column(name = "ACTION_STATE")
	private String state;

	@Column(name = "USER")
	private String user;

	@Column(name = "INPUT")
	@Lob
	@Basic(fetch = FetchType.LAZY)
	private byte[] input;

	@Column(name = "RESULT")
	@Lob
	@Basic(fetch = FetchType.LAZY)
	private byte[] result;

	@Column(name = "MESSAGES")
	@Lob
	@Basic(fetch = FetchType.LAZY)
	private byte[] messages;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "START")
	private Date start;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "FINISH")
	private Date finish;

	@Version
	@Column(name = "LAST_MODIFIED")
	private Timestamp lastModified;

	public long getActionId() {
		return actionId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	@Override
	public String nodeId() {
		return nodeId;
	}

	public Document getInput() {
		if (input != null) {
			DocumentBuilder db;
			try {
				db = domFactory.newDocumentBuilder();
				return db.parse(new ByteArrayInputStream(input));
			} catch (Exception pe) {
				pe.printStackTrace();
			}
		}
		return null;

	}

	public void setInput(Document doc) throws PlatformException {
		try {
			Transformer tform = transformFactory.newTransformer();
			tform.setOutputProperty(OutputKeys.INDENT, "yes");
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			tform.transform(new DOMSource(doc), new StreamResult(bos));
			input = bos.toByteArray();
		} catch (Exception e) {
			throw new PlatformException(e);
		}
	}

	public Timestamp getLastModified() {
		return lastModified;
	}

	@Override
	public ActionId id() {
		return new ActionIdImpl(actionId);
	}

	@Override
	public QName name() {
		return QName.valueOf(name);
	}

	public void setName(QName name) {
		this.name = name.toString();
	}

	@Override
	public QName component() {
		return QName.valueOf(component);
	}

	public void setComponent(QName component) {
		this.component = component.toString();
	}

	public void setActionType(QName actionType) {
		this.name = actionType.toString();
	}

	@Override
	public ActionState state() {
		if (state != null) {
			return ActionState.valueOf(state);
		}
		return null;
	}

	public void setState(ActionState state) {
		this.state = state.name();
	}

	@Override
	public List<ActionMessage> messages() {
		return Collections.unmodifiableList(getMessages());
	}
	
	public List<ActionMessage> getMessages() {
		if (messages != null) {
			try {
				Unmarshaller u = Cluster.CLUSTER_JAXB_CTX.createUnmarshaller();
				JAXBElement<Messages> logs = (JAXBElement<Messages>) u.unmarshal(new ByteArrayInputStream(messages));
				return convertMessages(logs.getValue().getMessage());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return new ArrayList<ActionMessage>();
	}

	public void setMessages(List<ActionMessage> messages) throws PlatformException {
		try {
			List<Message> newMessages = convertActionMessages(messages);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			Marshaller u = Cluster.CLUSTER_JAXB_CTX.createMarshaller();
			u.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			Messages newMessagesHolder = new Messages();
			newMessagesHolder.getMessage().addAll(newMessages);
			ObjectFactory factory = new ObjectFactory();
			JAXBElement<Messages> element = factory.createMessages(newMessagesHolder);
			u.marshal(element, bos);
			this.messages = bos.toByteArray();
		} catch (Exception e) {
			throw new PlatformException(e);
		}
	}

	@Override
	public Document result() {
		if (result != null) {
			DocumentBuilder db;
			try {
				db = domFactory.newDocumentBuilder();
				return db.parse(new ByteArrayInputStream(result));
			} catch (Exception pe) {
				pe.printStackTrace();
			}
		}
		return null;

	}

	public void setResult(Document doc) throws PlatformException {
		try {
			Transformer tform = transformFactory.newTransformer();
			tform.setOutputProperty(OutputKeys.INDENT, "yes");
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			tform.transform(new DOMSource(doc), new StreamResult(bos));
			result = bos.toByteArray();
		} catch (Exception e) {
			throw new PlatformException(e);
		}
	}

	@Override
	public Date start() {
		return start;
	}

	public void setStart(Date start) {
		this.start = start;
	}

	public void setFinish(Date finish) {
		this.finish = finish;
	}

	@Override
	public Date finish() {
		return finish;
	}

	public static List<Message> convertActionMessages(List<ActionMessage> messages) {
		List<Message> newMessages = new ArrayList<Message>();
		for (ActionMessage message : messages) {
			Message newMessage = new Message();
			Calendar timeStamp = Calendar.getInstance();
			timeStamp.setTime(message.getTimestamp());
			newMessage.setTimestamp(timeStamp);
			newMessage.setValue(message.getMessage());
			switch (message.getType()) {
			case INFO:
				newMessage.setType(LogType.INFO);
				break;
			case WARNING:
				newMessage.setType(LogType.WARNING);
				break;
			case ERROR:
				newMessage.setType(LogType.ERROR);
				break;
			}
			newMessages.add(newMessage);
		}
		return newMessages;
	}

	public static List<ActionMessage> convertMessages(List<Message> messages) {
		List<ActionMessage> newMessages = new ArrayList<ActionMessage>();
		for (Message message : messages) {
			ActionMessage newMessage = null;
			switch (message.getType()) {
			case INFO:
				newMessage = new ActionMessage(message.getTimestamp().getTime(), ActionMessage.LogType.INFO, message.getValue());
				break;
			case WARNING:
				newMessage = new ActionMessage(message.getTimestamp().getTime(), ActionMessage.LogType.WARNING, message.getValue());
				break;
			case ERROR:
				newMessage = new ActionMessage(message.getTimestamp().getTime(), ActionMessage.LogType.ERROR, message.getValue());
				break;
			}
			newMessages.add(newMessage);
		}
		return newMessages;
	}

}
