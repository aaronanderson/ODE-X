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
package org.apache.ode.runtime.exec.platform.task;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.ode.runtime.exec.platform.MessageImpl;
import org.apache.ode.runtime.exec.platform.target.TargetImpl;
import org.apache.ode.spi.exec.Message;
import org.apache.ode.spi.exec.PlatformException;
import org.apache.ode.spi.exec.target.Target;
import org.apache.ode.spi.exec.task.Task;
import org.apache.ode.spi.exec.task.TaskAction;
import org.w3c.dom.Document;

//@NamedQueries({ @NamedQuery(name = "localTasks", query = "select action from Action action where action.nodeId = :nodeId and action.state = 'SUBMIT'  or ( action.state = 'CANCELED' and action.finish is null )") })
@Entity
@Table(name = "TASK")
public class TaskImpl implements Task, Serializable {

	private static final Logger log = Logger.getLogger(TaskImpl.class.getName());
	public static DocumentBuilderFactory domFactory;
	public static TransformerFactory transformFactory;
	static {
		domFactory = DocumentBuilderFactory.newInstance();
		domFactory.setNamespaceAware(true);
		transformFactory = TransformerFactory.newInstance();
	}

	@Transient
	EntityManagerFactory pmgrFactory;

	@Id
	@GeneratedValue
	@Column(name = "TASK_ID")
	private long taskId;

	@Column(name = "NODE_ID")
	private String nodeId;

	@Column(name = "NAME")
	private String name;

	@Column(name = "COMPONENT")
	private String component;

	@Column(name = "STATE")
	private String state;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinTable(name = "TASK_TARGET", joinColumns = @JoinColumn(name = "TASK_ID"), inverseJoinColumns = {
			@JoinColumn(name = "TARGET_ID", referencedColumnName = "ID"), @JoinColumn(name = "TARGET_TYPE", referencedColumnName = "TYPE") })
	Set<TargetImpl> targets;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinTable(name = "TASK_MESSAGE", joinColumns = @JoinColumn(name = "TASK_ID"), inverseJoinColumns = @JoinColumn(name = "MESSAGE_ID"))
	private List<MessageImpl> messages;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinTable(name = "TASK_ACTIONS", joinColumns = @JoinColumn(name = "TASK_ID"), inverseJoinColumns = @JoinColumn(name = "ACTION_ID"))
	Set<TaskActionImpl> actions;

	@Column(name = "USER")
	private String user;

	@Column(name = "INPUT")
	@Lob
	@Basic(fetch = FetchType.LAZY)
	private byte[] input;

	@Column(name = "OUTPUT")
	@Lob
	@Basic(fetch = FetchType.LAZY)
	private byte[] output;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "START")
	private Date start;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "FINISH")
	private Date finish;

	@Version
	@Column(name = "LAST_MODIFIED")
	private Timestamp lastModified;

	@Override
	public void refresh() {

	}

	public long getTaskId() {
		return taskId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	@Override
	public String nodeId() {
		return nodeId;
	}

	@Override
	public QName name() {
		return QName.valueOf(name);
	}

	public void setName(QName name) {
		this.name = name.toString();
	}

	@Override
	public TaskId id() {
		return new TaskIdImpl(taskId);
	}

	public void setTaskId(long taskId) {
		this.taskId = taskId;
	}

	@Override
	public QName component() {
		return QName.valueOf(component);
	}

	public void setComponent(QName component) {
		this.component = component.toString();
	}

	@Override
	public TaskState state() {
		if (state != null) {
			return TaskState.valueOf(state);
		}
		return null;
	}

	public void setState(TaskState state) {
		this.state = state.name();
	}

	public void setTargets(Set<TargetImpl> targets) {
		this.targets = targets;
	}

	public Set<TargetImpl> getTargets() {
		return targets;
	}

	@Override
	public Set<Target> targets() {
		return (Set<Target>) (Object) getTargets();
	}

	public void setMessages(List<MessageImpl> messages) {
		this.messages = messages;
	}

	public List<MessageImpl> getMessages() {
		return messages;
	}

	@Override
	public List<Message> messages() {
		return (List<Message>) (Object) getMessages();
	}

	public void setTaskAction(Set<TaskActionImpl> actions) {
		this.actions = actions;
	}

	public Set<TaskActionImpl> getActions() {
		return actions;
	}

	public void setActions(Set<TaskActionImpl> actions) {
		this.actions = actions;
	}

	@Override
	public Set<TaskAction> actions() {
		return (Set<TaskAction>) (Object) getActions();
	}

	@Override
	public Document input() {
		return getDOMInput();
	}

	public Document getDOMInput() {
		return contentToDom(getInput());
	}

	public void setDOMInput(Document doc) throws PlatformException {
		setInput(domToContent(doc));
	}

	public byte[] getInput() {//need to use getter methods for JPA lazy loading
		return input;
	}

	public void setInput(byte[] input) {//need to use getter methods for JPA lazy loading
		this.input = input;
	}

	/*
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
				log.log(Level.SEVERE,"",e);
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
	}*/

	@Override
	public Document output() {
		return getDOMOutput();
	}

	public Document getDOMOutput() {
		return contentToDom(getOutput());
	}

	public void setDOMOutput(Document doc) throws PlatformException {
		setOutput(domToContent(doc));
	}

	public byte[] getOutput() {//need to use getter methods for JPA lazy loading
		return output;
	}

	public void setOutput(byte[] output) {//need to use getter methods for JPA lazy loading
		this.output = output;
	}

	public static byte[] domToContent(Document doc) throws PlatformException {
		if (doc != null) {
			try {
				Transformer tform = transformFactory.newTransformer();
				tform.setOutputProperty(OutputKeys.INDENT, "yes");
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				tform.transform(new DOMSource(doc), new StreamResult(bos));
				return bos.toByteArray();
			} catch (Exception e) {
				throw new PlatformException(e);
			}
		} else {
			return null;
		}
	}

	public static Document contentToDom(byte[] content) {
		if (content != null) {
			DocumentBuilder db;
			try {
				db = domFactory.newDocumentBuilder();
				return db.parse(new ByteArrayInputStream(content));
			} catch (Exception pe) {
				log.log(Level.SEVERE, "", pe);
			}
		}
		return null;
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

	@Override
	public Date modified() {
		return lastModified;
	}

	/*
	public static List<Message> convertActionMessages(List<ActionMessage> messages) {
		List<Message> newMessages = new ArrayList<Message>();
		for (ActionMessage message : messages) {
			//Message newMessage = new Message();
			Calendar timeStamp = Calendar.getInstance();
			timeStamp.setTime(message.timestamp());
			//newMessage.setTimestamp(timeStamp);
			//newMessage.setValue(message.message());
			switch (message.level()) {
			case INFO:
				//newMessage.setType(LogType.INFO);
				break;
			case WARNING:
				//newMessage.setType(LogType.WARNING);
				break;
			case ERROR:
				//newMessage.setType(LogType.ERROR);
				break;
			}
			newMessages.add(newMessage);
		}
		return newMessages;
	}
	*/
	/*
		public static List<ActionMessage> convertMessages(List<Message> messages) {
			List<ActionMessage> newMessages = new ArrayList<ActionMessage>();
			for (Message message : messages) {
				ActionMessage newMessage = null;
				switch (message.level()) {
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
	*/

	public static class TaskIdImpl implements TaskId {

		final long taskId;

		public TaskIdImpl(long taskId) {
			this.taskId = taskId;
		}

		public long id() {
			return taskId;
		}

	}

}
