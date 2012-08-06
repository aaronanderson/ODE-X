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

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.ode.runtime.exec.platform.target.TargetAllImpl;
import org.apache.ode.runtime.exec.platform.target.TargetClusterImpl;
import org.apache.ode.runtime.exec.platform.target.TargetImpl.TargetPK;
import org.apache.ode.runtime.exec.platform.target.TargetNodeImpl;
import org.apache.ode.runtime.exec.platform.task.TaskCallable.TaskResult;
import org.apache.ode.spi.exec.Message.LogLevel;
import org.apache.ode.spi.exec.Message.MessageListener;
import org.apache.ode.spi.exec.Platform;
import org.apache.ode.spi.exec.PlatformException;
import org.apache.ode.spi.exec.Program;
import org.apache.ode.spi.exec.target.Target;
import org.apache.ode.spi.exec.target.TargetAll;
import org.apache.ode.spi.exec.target.TargetCluster;
import org.apache.ode.spi.exec.target.TargetNode;
import org.apache.ode.spi.exec.task.Task;
import org.apache.ode.spi.exec.task.Task.TaskId;
import org.apache.ode.spi.exec.task.TaskCallback;
import org.apache.ode.spi.repo.Artifact;
import org.apache.ode.spi.repo.ArtifactDataSource;
import org.apache.ode.spi.repo.DataHandler;
import org.apache.ode.spi.repo.Repository;
import org.apache.ode.spi.repo.XMLDataContentHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PlatformImpl implements Platform {

	//@PersistenceContext(unitName = "platform")
	private EntityManager pmgr;

	//@Inject
	Repository repo;
	//@Inject
	Provider<ArtifactDataSource> dsProvider;

	//@Inject
	private NodeImpl node;

	LogLevel logLevel = LogLevel.WARNING;
	CopyOnWriteArraySet<MessageListener> taskListeners = new CopyOnWriteArraySet<MessageListener>();

	public class PlatformListener {

	}

	private PlatformImpl(NodeImpl node, Repository repo, Provider<ArtifactDataSource> dsProvider, EntityManager pmgr) {
		this.node = node;
		this.repo = repo;
		this.dsProvider = dsProvider;
		this.pmgr = pmgr;
	}

	//State Methods

	public void login(AuthContext authContext) throws PlatformException {

	}

	public void logout() throws PlatformException {

	}

	public void registerListener(MessageListener listener) {
		node.addListener(listener);
	}

	public void unregisterListener(MessageListener listener) {
		node.removeListener(listener);
	}

	public void setLogLevel(LogLevel level) {
		this.logLevel = logLevel;
	}

	//Platform Methods

	@Override
	public Document setup(Artifact... executables) throws PlatformException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder db = factory.newDocumentBuilder();
			Document programConfiguration = db.newDocument();

			for (Artifact ex : executables) {
				ArtifactDataSource ds = dsProvider.get();
				ds.configure(ex);
				DataHandler dh = repo.getDataHandler(ds);
				Document exec = (Document) dh.getTransferData(XMLDataContentHandler.DOM_FLAVOR);
				NodeList list = exec.getDocumentElement().getChildNodes();
				int eCount = 0;
				for (int i = 0; i < list.getLength(); i++) {
					Node n = list.item(i);
					if (n instanceof Element) {
						Element e = (Element) n;
						if ("installation".equals(e.getLocalName())) {
							programConfiguration.appendChild(programConfiguration.adoptNode(e.cloneNode(true)));
						} else if (++eCount > 2) {// if present should be 1st or 2nd
													// child element
							return null;
						}
					}

				}
			}
			return programConfiguration;
		} catch (Exception e) {
			throw new PlatformException(e);
		}

	}

	@Override
	public <T extends Target> T createTarget(String id, Class<T> type) throws PlatformException {
		if (type == null) {
			throw new PlatformException("Target type is null");
		}
		T target = null;
		if (TargetAll.class.equals(type)) {
			target = (T) pmgr.find(TargetAllImpl.class, new TargetPK(TargetAllImpl.TYPE, TargetAllImpl.TYPE));
		} else if (TargetNode.class.equals(type)) {
			target = (T) pmgr.find(TargetNodeImpl.class, new TargetPK(id, TargetNodeImpl.TYPE));
		} else if (TargetCluster.class.equals(type)) {
			target = (T) pmgr.find(TargetClusterImpl.class, new TargetPK(id, TargetClusterImpl.TYPE));
		} else {
			throw new PlatformException(String.format("Invalid target type %s", type.getName()));
		}
		if (target == null) {
			throw new PlatformException(String.format("Invalid target %s:%s", id, type.getName()));
		}
		pmgr.detach(target);
		return target;
	}

	@Override
	public void install(QName id, Document programConfiguration, Target... targets) throws PlatformException {
		// cluster.execute(action, installData,targets);
	}

	@Override
	public Program programInfo(QName id) throws PlatformException {
		return pmgr.find(ProgramImpl.class, id.toString());

	}

	@Override
	public void start(QName id, Target... targets) throws PlatformException {
		//return null;
	}

	@Override
	public void stop(QName id, Target... targets) throws PlatformException {
	}

	@Override
	public void uninstall(QName id, Target... targets) throws PlatformException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		DocumentBuilder db = null;
		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new PlatformException(e);
		}
		Document taskInput = db.newDocument();
		Element root = taskInput.createElementNS(PlatformTask.UNINSTALL_TASK.qname().getNamespaceURI(), "taskId");
		root.setTextContent(id.toString());
		taskInput.appendChild(root);
		node.executeSync(PlatformTask.UNINSTALL_TASK.qname(), logLevel, taskInput, null, targets);

	}

	@Override
	public TaskId executeAsync(QName task, Document taskInput, TaskCallback<?, ?> callback, Target... targets) throws PlatformException {
		return node.executeAsyncId(task, logLevel, taskInput, callback, targets);
	}

	@Override
	public Future<Document> execute(QName task, Document taskInput, TaskCallback<?, ?> callback, Target... targets) throws PlatformException {
		final Future<TaskResult> taskResult = node.executeAsyncFuture(task, logLevel, taskInput, callback, targets);
		return new Future<Document>() {

			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				return taskResult.cancel(mayInterruptIfRunning);
			}

			@Override
			public boolean isCancelled() {
				return taskResult.isCancelled();
			}

			@Override
			public boolean isDone() {
				return taskResult.isDone();
			}

			@Override
			public Document get() throws InterruptedException, ExecutionException {
				return taskResult.get().output;
			}

			@Override
			public Document get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
				return taskResult.get(timeout, unit).output;
			}

		};
	}

	@Override
	public Task taskStatus(TaskId taskId) throws PlatformException {
		return node.status(taskId);
	}

	@Override
	public void cancel(TaskId taskId) throws PlatformException {
		node.cancel(taskId);
	}

	@Override
	public Set<NodeStatus> status() {
		return node.status();
	}

	@Singleton
	public static class PlatformProvider implements Provider<Platform> {

		@PersistenceContext(unitName = "platform")
		private EntityManager pmgr;

		@Inject
		Repository repo;

		@Inject
		Provider<ArtifactDataSource> dsProvider;

		@Inject
		private NodeImpl node;

		@Override
		public Platform get() {
			return new PlatformImpl(node, repo, dsProvider, pmgr);
		}

	}

}
