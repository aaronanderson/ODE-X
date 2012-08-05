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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.DataSource;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Qualifier;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.ode.runtime.exec.JAXBRuntimeUtil;
import org.apache.ode.runtime.exec.cluster.xml.ClusterConfig;
import org.apache.ode.runtime.exec.platform.task.TaskCallable.TaskResult;
import org.apache.ode.runtime.exec.platform.task.TaskExecutor;
import org.apache.ode.runtime.exec.platform.task.TaskImpl;
import org.apache.ode.runtime.exec.platform.task.TaskImpl.TaskIdImpl;
import org.apache.ode.spi.exec.Component;
import org.apache.ode.spi.exec.Component.InstructionSet;
import org.apache.ode.spi.exec.Executors;
import org.apache.ode.spi.exec.Message;
import org.apache.ode.spi.exec.Message.LogLevel;
import org.apache.ode.spi.exec.Message.MessageEvent;
import org.apache.ode.spi.exec.Message.MessageListener;
import org.apache.ode.spi.exec.Node;
import org.apache.ode.spi.exec.Platform.NodeStatus;
import org.apache.ode.spi.exec.Platform.NodeStatus.NodeState;
import org.apache.ode.spi.exec.PlatformException;
import org.apache.ode.spi.exec.target.Target;
import org.apache.ode.spi.exec.task.Task;
import org.apache.ode.spi.exec.task.Task.TaskId;
import org.apache.ode.spi.exec.task.TaskAction;
import org.apache.ode.spi.exec.task.TaskActionDefinition;
import org.apache.ode.spi.exec.task.TaskCallback;
import org.apache.ode.spi.exec.task.TaskDefinition;
import org.apache.ode.spi.exec.task.TaskException;
import org.apache.ode.spi.exec.xml.Executable;
import org.apache.ode.spi.exec.xml.InstructionSets;
import org.apache.ode.spi.repo.JAXBDataContentHandler;
import org.apache.ode.spi.repo.Repository;
import org.apache.ode.spi.repo.RepositoryException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

@Singleton
public class NodeImpl implements Node, MessageListener {

	public static JAXBContext CLUSTER_JAXB_CTX;
	private static final Logger log = Logger.getLogger(NodeImpl.class.getName());

	public static final Logger msgLog = Logger.getLogger("org.apache.ode.runtime.exec.platform.Message");

	static {
		try {
			CLUSTER_JAXB_CTX = JAXBContext.newInstance("org.apache.ode.runtime.exec.cluster.xml");
		} catch (JAXBException je) {
			log.log(Level.SEVERE, "", je);
		}
	}

	@PersistenceUnit(unitName = "platform")
	EntityManagerFactory pmgrFactory;

	@Inject
	ClusterConfig config;

	@ClusterId
	String clusterId;

	@NodeId
	String nodeId;

	@Inject
	Repository repo;

	@Inject
	HealthCheck healthCheck;

	@Inject
	MessageHandler messageHandler;

	@Inject
	TaskExecutor taskExec;

	@LocalNodeState
	AtomicReference<NodeState> localNodeState;

	private Map<QName, Component> components = new ConcurrentHashMap<QName, Component>();
	private Map<QName, InstructionSet> instructions = new ConcurrentHashMap<QName, InstructionSet>();
	private Map<QName, TaskDefinition<?, ?>> tasks = new ConcurrentHashMap<QName, TaskDefinition<?, ?>>();
	private Map<QName, TaskActionDefinition<?, ?>> actions = new ConcurrentHashMap<QName, TaskActionDefinition<?, ?>>();

	private QName architecture;

	private LogLevel logLevel = LogLevel.WARNING;

	@Inject
	Executors executors;

	@Singleton
	public static class LocalNodeStateProvider implements Provider<AtomicReference<NodeState>> {
		AtomicReference<NodeState> localNodeState;

		public LocalNodeStateProvider() {
			localNodeState = new AtomicReference<NodeState>();
			localNodeState.set(NodeState.OFFLINE);
		}

		@Override
		public AtomicReference<NodeState> get() {
			return localNodeState;
		}

	}

	@PostConstruct
	public void init() {
		log.fine("Initializing Node");
		taskExec.configure(this);
		localNodeState.set(NodeState.OFFLINE);

		// Prime the health check to make sure it runs at least once before
		// continuing startup
		// healthCheck.run();
		ScheduledExecutorService clusterScheduler;
		try {
			clusterScheduler = executors.initClusterTaskScheduler();
			clusterScheduler.scheduleAtFixedRate(healthCheck, 0, config.getHealthCheck().getFrequency(), TimeUnit.MILLISECONDS);
			clusterScheduler.scheduleAtFixedRate(taskExec, 0, config.getTaskCheck().getFrequency(), TimeUnit.MILLISECONDS);
			clusterScheduler.scheduleAtFixedRate(messageHandler, 0, config.getMessageCheck().getFrequency(), TimeUnit.MILLISECONDS);
		} catch (PlatformException pe) {
			log.log(Level.SEVERE, "", pe);
		}
		messageHandler.addListener(this);

		log.fine("Cluster Initialized");
	}

	@PreDestroy
	public void destroy() {
		messageHandler.removeListener(this);
		try {
			executors.destroyClusterTaskScheduler();
		} catch (PlatformException pe) {
			log.log(Level.SEVERE, "", pe);
		}

	}

	//Message methods for local logging
	//Message methods
	@Override
	public LogLevel levelFilter() {
		return logLevel;
	}

	@Override
	public void message(MessageEvent message) {
		switch (message.message().level()) {
		//TODO print timestamp
		case DEBUG:
			msgLog.fine(String.format("%00d: %s", message.message().code(), message.message().message()));
			break;
		case INFO:
			msgLog.info(String.format("%00d: %s", message.message().code(), message.message().message()));
			break;
		case WARNING:
			msgLog.warning(String.format("%00d: %s", message.message().code(), message.message().message()));
			break;
		case ERROR:
			msgLog.severe(String.format("%00d: %s", message.message().code(), message.message().message()));
			break;
		}

	}

	@Override
	public QName architecture() {
		return architecture;
	}

	public void setArchitecture(QName architecture) {
		this.architecture = architecture;
	}

	@Override
	public Set<QName> getComponents() {
		return Collections.unmodifiableSet(components.keySet());
	}

	@Override
	public void registerComponent(Component component) {
		components.put(component.name(), component);
		for (InstructionSet is : component.instructionSets()) {
			instructions.put(is.getName(), is);
		}
		for (TaskDefinition<?, ?> td : component.tasks()) {
			tasks.put(td.task(), td);
		}
		for (TaskActionDefinition<?, ?> tad : component.actions()) {
			actions.put(tad.action(), tad);
		}

	}

	@Override
	public void unregisterComponent(Component component) {

	}

	@Override
	public Map<QName, InstructionSet> getInstructionSets() {
		return instructions;
	}

	@Override
	public Map<QName, TaskDefinition<?, ?>> getTaskDefinitions() {
		return tasks;
	}

	@Override
	public Map<QName, TaskActionDefinition<?, ?>> getTaskActionDefinitions() {
		return actions;
	}

	@Override
	public void online() throws PlatformException {
		taskExec.setupTasks(components.values());
		taskExec.online();
		localNodeState.set(NodeState.ONLINE);
		healthCheck.run();
	}

	@Override
	public void offline() throws PlatformException {
		taskExec.offline();
		localNodeState.set(NodeState.OFFLINE);
		healthCheck.run();
	}

	public void executeSync(QName task, LogLevel logLevel, Document taskInput, TaskCallback<?, ?> callback, Target... targets) throws TaskException {
		Future<TaskResult> result = taskExec.submitTask(logLevel, null, null, task, taskInput != null ? taskInput.getDocumentElement() : null, callback,
				targets).result;
		try {
			result.get();
		} catch (InterruptedException | ExecutionException e) {
			throw new TaskException(e);
		}
	}

	public TaskIdImpl executeAsyncId(QName task, LogLevel logLevel, Document taskInput, TaskCallback<?, ?> callback, Target... targets) throws TaskException {
		return taskExec.submitTask(logLevel, null, null, task, taskInput != null ? taskInput.getDocumentElement() : null, callback, targets).id;
	}

	public Future<TaskResult> executeAsyncFuture(QName task, LogLevel logLevel, Document taskInput, TaskCallback<?, ?> callback, Target... targets)
			throws TaskException {
		return taskExec.submitTask(logLevel, null, null, task, taskInput != null ? taskInput.getDocumentElement() : null, callback, targets).result;

	}

	public Task status(TaskId taskId) throws PlatformException {
		if (taskId == null) {
			throw new PlatformException("null taskId");
		}
		TaskFascade task = new TaskFascade(((TaskIdImpl) taskId).id(), pmgrFactory);
		task.refresh();
		return task;

	}

	public static class TaskFascade implements Task {
		final EntityManagerFactory pmgrFactory;
		final long id;
		TaskImpl impl;

		public TaskFascade(long id, EntityManagerFactory pmgrFactory) {
			this.id = id;
			this.pmgrFactory = pmgrFactory;
		}

		@Override
		public String nodeId() {
			return impl.nodeId();
		}

		@Override
		public void refresh() {
			//for lazy load fields we will need to return the value while the entity is still attached
			EntityManager pmgr = pmgrFactory.createEntityManager();
			try {
				TaskImpl impl = pmgr.find(TaskImpl.class, id);
				pmgr.detach(impl);
				this.impl = impl;
			} catch (Throwable t) {
				log.log(Level.SEVERE, "", t);
			} finally {
				pmgr.close();
			}
		}

		@Override
		public QName name() {
			return impl.name();
		}

		@Override
		public TaskId id() {
			return impl.id();
		}

		@Override
		public QName component() {
			return impl.component();
		}

		@Override
		public TaskState state() {
			return impl.state();
		}

		@Override
		public Set<Target> targets() {
			EntityManager pmgr = pmgrFactory.createEntityManager();
			try {
				TaskImpl impl = pmgr.find(TaskImpl.class, id);
				this.impl = impl;
				return impl.targets();
			} catch (Throwable t) {
				log.log(Level.SEVERE, "", t);
				return null;
			} finally {
				pmgr.close();
			}

		}

		@Override
		public List<Message> messages() {
			EntityManager pmgr = pmgrFactory.createEntityManager();
			try {
				TaskImpl impl = pmgr.find(TaskImpl.class, id);
				this.impl = impl;
				return impl.messages();
			} catch (Throwable t) {
				log.log(Level.SEVERE, "", t);
				return null;
			} finally {
				pmgr.close();
			}
		}

		@Override
		public Set<TaskAction> actions() {
			EntityManager pmgr = pmgrFactory.createEntityManager();
			try {
				TaskImpl impl = pmgr.find(TaskImpl.class, id);
				this.impl = impl;
				return impl.actions();
			} catch (Throwable t) {
				log.log(Level.SEVERE, "", t);
				return null;
			} finally {
				pmgr.close();
			}
		}

		@Override
		public Document input() {
			EntityManager pmgr = pmgrFactory.createEntityManager();
			try {
				TaskImpl impl = pmgr.find(TaskImpl.class, id);
				this.impl = impl;
				return impl.input();
			} catch (Throwable t) {
				log.log(Level.SEVERE, "", t);
				return null;
			} finally {
				pmgr.close();
			}
		}

		@Override
		public Document output() {
			EntityManager pmgr = pmgrFactory.createEntityManager();
			try {
				TaskImpl impl = pmgr.find(TaskImpl.class, id);
				this.impl = impl;
				return impl.output();
			} catch (Throwable t) {
				log.log(Level.SEVERE, "", t);
				return null;
			} finally {
				pmgr.close();
			}
		}

		@Override
		public Date start() {
			return impl.start();
		}

		@Override
		public Date finish() {
			return impl.finish();
		}

		@Override
		public Date modified() {
			return impl.modified();
		}

	}

	public void cancel(TaskId taskId) throws PlatformException {
		//actionExec.cancel((ActionIdImpl) actionId);
	}

	public void addListener(MessageListener listener) {
		messageHandler.addListener(listener);
	}

	public void removeListener(MessageListener listener) {
		messageHandler.removeListener(listener);
	}

	/**
	 * Available targets: LOCAL, CLUSTER, NODE
	 * 
	 * @param targets
	 * @return
	 * @throws PlatformException
	 */
	public String getTarget(Target[] targets) throws PlatformException {
		/*
		if (targets == null) {
			targets = new Target[] { new Target(null, TargetType.LOCAL) };
		}

		if (targets.length > 1) {
			throw new PlatformException("Actions may only have a single target");
		}

		if (TargetType.ALL.equals(targets[0].getTargetType())) {
			throw new PlatformException("ALL target not support for ActionTask type Action");
		}
		if (TargetType.LOCAL.equals(targets[0].getTargetType())) {
			return nodeId;
		}

		Set<String> canidates = new HashSet();
		for (NodeStatus n : healthCheck.availableNodes()) {
			if (NodeState.ONLINE.equals(n.state())) {
				for (Target t : targets) {
					switch (t.getTargetType()) {
					case CLUSTER:
						if (n.clusterId().equals(t.getName())) {
							canidates.add(n.nodeId());
						}
						break;
					case NODE:
						if (n.nodeId().equals(t.getName())) {
							canidates.add(n.nodeId());
						}
						break;
					}
				}
			}
		}
		if (canidates.size() == 0) {
			throw new PlatformException(String.format("Invalid target %s %s", targets[0].getTargetType(), targets[0].getName()));
		}
		if (canidates.contains(nodeId)) {
			return nodeId;
		}

		return canidates.iterator().next();
		*/
		return null;
	}

	public Set<NodeStatus> status() {
		return healthCheck.availableNodes();
	}

	@Qualifier
	@java.lang.annotation.Target({ ElementType.FIELD, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface ClusterId {

	}

	@Qualifier
	@java.lang.annotation.Target({ ElementType.FIELD, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface NodeId {

	}

	@Qualifier
	@java.lang.annotation.Target({ ElementType.FIELD, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface LocalNodeState {

	}

	@Qualifier
	@java.lang.annotation.Target({ ElementType.FIELD, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface NodeCheck {

	}

	@Qualifier
	@java.lang.annotation.Target({ ElementType.FIELD, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface TaskCheck {

	}

	@Qualifier
	@java.lang.annotation.Target({ ElementType.FIELD, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface MessageCheck {

	}

	@Singleton
	public static class ClusterConfigProvider implements Provider<ClusterConfig> {
		@ClusterId
		private String clusterId;

		@NodeId
		private String nodeId;

		@Inject
		Repository repo;

		private ClusterConfig config;

		public ClusterConfig get() {
			return config;
		}

		@PostConstruct
		public void init() throws Exception {
			log.fine("Initializing ClusterConfigProvider");
			repo.registerNamespace(CLUSTER_NAMESPACE, CLUSTER_MIMETYPE);
			repo.registerHandler(CLUSTER_MIMETYPE, new JAXBDataContentHandler(CLUSTER_JAXB_CTX) {
				@Override
				public QName getDefaultQName(DataSource dataSource) {
					QName defaultName = null;
					try {
						InputStream is = dataSource.getInputStream();
						XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(is);
						reader.nextTag();
						String tns = CLUSTER_NAMESPACE;
						String name = reader.getAttributeValue(null, "name");
						reader.close();
						if (name != null) {
							defaultName = new QName(tns, name);
						}
						return defaultName;
					} catch (Exception e) {
						return null;
					}
				}

			});

			QName configName = new QName(CLUSTER_NAMESPACE, clusterId);
			try {
				JAXBElement<ClusterConfig> config = repo.read(configName, CLUSTER_MIMETYPE, "1.0", JAXBElement.class);
				this.config = config.getValue();
				return;
			} catch (RepositoryException e) {

			}
			log.log(Level.WARNING, "Unable to load cluster config, using default config");
			try {
				Unmarshaller u = CLUSTER_JAXB_CTX.createUnmarshaller();
				JAXBElement<ClusterConfig> config = (JAXBElement<ClusterConfig>) u.unmarshal(getClass().getResourceAsStream("/META-INF/default_cluster.xml"));
				repo.create(configName, CLUSTER_MIMETYPE, "1.0", config);
				this.config = config.getValue();
			} catch (Exception e) {
				log.log(Level.SEVERE, "", e);
				throw e;
			}
			if (!config.getNodes().isAutoDiscovery() && !config.getNodes().getNode().contains(nodeId)) {
				log.log(Level.SEVERE, "Node auth discovery disabled and nodeId {0} is undeclared, aborting", nodeId);
				throw new Exception(String.format("Undefined node %s in cluster %s", nodeId, clusterId));
			}
			if (config.getHealthCheck() == null) {
				config.setHealthCheck(new org.apache.ode.runtime.exec.cluster.xml.HealthCheck());
			}
			if (config.getTaskCheck() == null) {
				config.setTaskCheck(new org.apache.ode.runtime.exec.cluster.xml.TaskCheck());
			}
			if (config.getMessageCheck() == null) {
				config.setMessageCheck(new org.apache.ode.runtime.exec.cluster.xml.MessageCheck());
			}

		}
	}

	@Singleton
	public static class ClusterXMLDSig {

		@Inject
		ClusterConfig clusterConfig;

		static XMLSignatureFactory xmlFactory = XMLSignatureFactory.getInstance("DOM");

		String encKey;
		KeyPair clusterKey;

		@PostConstruct
		public void init() {
			try {
				encKey = clusterConfig.getSecurity().getEncKey();

				KeyStore ks = KeyStore.getInstance("PKCS12");
				ks.load(new ByteArrayInputStream(clusterConfig.getSecurity().getEncKeyStore()), clusterConfig.getSecurity().getEncKeyStorePass().toCharArray());
				PrivateKey privateKey = (PrivateKey) ks.getKey(clusterConfig.getSecurity().getKeyAlias(), clusterConfig.getSecurity().getEncKeyPass()
						.toCharArray());
				if (privateKey == null) {
					throw new Exception(String.format("Can't find private key alias %s in cluster config keystore", clusterConfig.getSecurity().getKeyAlias()));
				}
				java.security.cert.Certificate cert = ks.getCertificate(clusterConfig.getSecurity().getKeyAlias());
				if (cert == null) {
					throw new Exception(String.format("Can't find public key alias %s in cluster config keystore", clusterConfig.getSecurity().getKeyAlias()));
				}
				PublicKey publicKey = cert.getPublicKey();
				clusterKey = new KeyPair(publicKey, privateKey);

			} catch (Exception e) {
				log.log(Level.SEVERE, "", e);
			}

		}

		public String encrypt(String cleartext) throws Exception {
			return encrypt(encKey, cleartext);
		}

		public String decrypt(String enctext) throws Exception {
			return decrypt(encKey, enctext);
		}

		public Document signXml(Document doc) throws Exception {
			return sign(doc, clusterKey, false);
		}

		public boolean validateXml(Document doc) throws Exception {
			return validate(doc, null);
		}

		public static Document sign(Document doc, KeyPair keyPair, boolean includeKeyInfo) throws Exception {

			DOMSignContext context = new DOMSignContext(keyPair.getPrivate(), doc.getDocumentElement());
			//Element dsig = doc.createElementNS(CLUSTER_NAMESPACE, "xmldsig");
			//doc.getDocumentElement().insertBefore(dsig, doc.getDocumentElement().getFirstChild());
			//context.setParent(dsig);
			context.setNextSibling(doc.getDocumentElement().getFirstChild());

			javax.xml.crypto.dsig.Reference ref = xmlFactory.newReference("", xmlFactory.newDigestMethod(DigestMethod.SHA1, null),
					Collections.singletonList(xmlFactory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)), null, null);

			SignedInfo si = xmlFactory.newSignedInfo(xmlFactory.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
					xmlFactory.newSignatureMethod(SignatureMethod.RSA_SHA1, null), Collections.singletonList(ref));
			KeyInfo ki = null;
			if (includeKeyInfo) {
				KeyInfoFactory kif = xmlFactory.getKeyInfoFactory();
				KeyValue kv = kif.newKeyValue(keyPair.getPublic());
				ki = kif.newKeyInfo(Collections.singletonList(kv));
			}
			XMLSignature signature = xmlFactory.newXMLSignature(si, ki);

			signature.sign(context);
			return doc;

		}

		public static boolean validate(Document doc, PublicKey key) throws Exception {
			//NodeList nl = doc.getElementsByTagNameNS(CLUSTER_NAMESPACE, "xmldsig");
			NodeList nl = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
			if (nl.getLength() == 0) {
				throw new Exception("Cannot find Signature element");
			}

			XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
			DOMValidateContext valContext = null;
			if (key != null) {
				valContext = new DOMValidateContext(key, nl.item(0));
			} else {
				valContext = new DOMValidateContext(new KeySelector() {
					public KeySelectorResult select(KeyInfo keyInfo, KeySelector.Purpose purpose, AlgorithmMethod method, XMLCryptoContext context)
							throws KeySelectorException {
						if (keyInfo == null) {
							throw new KeySelectorException("KeyInfo is null");
						}
						for (XMLStructure struct : (List<XMLStructure>) keyInfo.getContent()) {
							if (struct instanceof KeyValue) {
								try {
									final PublicKey primaryKey = ((KeyValue) struct).getPublicKey();
									return new KeySelectorResult() {

										@Override
										public Key getKey() {
											return primaryKey;
										}
									};
								} catch (KeyException ke) {
									throw new KeySelectorException(ke);
								}
							}
						}
						throw new KeySelectorException("KeyValue not found");
					}
				}, nl.item(0));
			}
			XMLSignature signature = fac.unmarshalXMLSignature(valContext);

			if (signature.validate(valContext)) {
				return true;
			}
			return false;

		}

		public static String encrypt(String b64EncKey, String cleartext) throws Exception {
			byte[] encKey = DatatypeConverter.parseBase64Binary(b64EncKey);
			SecretKeySpec key = new SecretKeySpec(encKey, "AES");
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, key);
			return DatatypeConverter.printBase64Binary(cipher.doFinal(cleartext.getBytes("UTF-8")));

		}

		public static String decrypt(String b64EncKey, String enctext) throws Exception {
			byte[] encKey = DatatypeConverter.parseBase64Binary(b64EncKey);
			SecretKeySpec key = new SecretKeySpec(encKey, "AES");
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, key);
			return DatatypeConverter.printBase64Binary(cipher.doFinal(enctext.getBytes("UTF-8")));

		}

		public static void main(String... args) {
			//generate the default keystore
			//keytool -genkeypair -keyalg RSA -keysize 512 -alias ode-x -dname "cn=ode-x,dc=apache,dc=org" -validity 999 -keypass apacheodex -keystore /tmp/ode-x.p12 -storepass apacheodex -storetype pkcs12
			//java -cp spi/target/ode-spi-0.1-SNAPSHOT.jar:runtime/target/classes org.apache.ode.runtime.exec.platform.NodeImpl\$ClusterXMLDSig -genEncKey
			//java -cp spi/target/ode-spi-0.1-SNAPSHOT.jar:runtime/target/classes org.apache.ode.runtime.exec.platform.NodeImpl\$ClusterXMLDSig -keystoreToXml /tmp/ode-x.p12
			//java -cp spi/target/ode-spi-0.1-SNAPSHOT.jar:runtime/target/classes org.apache.ode.runtime.exec.platform.NodeImpl\$ClusterXMLDSig -encrypt daN4eSIZ7G0YqUPs8taHzQ== apacheodex
			//java -cp spi/target/ode-spi-0.1-SNAPSHOT.jar:runtime/target/classes org.apache.ode.runtime.exec.platform.NodeImpl\$ClusterXMLDSig -sign /tmp/ode-x.p12 apacheodex apacheodex ode-x true /tmp/test.xml /tmp/test-sign.xml
			//java -cp spi/target/ode-spi-0.1-SNAPSHOT.jar:runtime/target/classes org.apache.ode.runtime.exec.platform.NodeImpl\$ClusterXMLDSig -verify false /tmp/ode-x.p12 apacheodex ode-x /tmp/test-sign.xml
			//java -cp spi/target/ode-spi-0.1-SNAPSHOT.jar:runtime/target/classes org.apache.ode.runtime.exec.platform.NodeImpl\$ClusterXMLDSig -verify true
			boolean showUsage = false;
			if (args.length > 0) {
				switch (args[0]) {
				case "-genEncKey":
					if (args.length != 1) {
						showUsage = true;
						break;
					}
					try {
						KeyGenerator kgen = KeyGenerator.getInstance("AES");
						kgen.init(128); // 192 and 256 require JCE export
						SecretKey skey = kgen.generateKey();
						System.out.format("New Encryption Key: %s\n", DatatypeConverter.printBase64Binary(skey.getEncoded()));
					} catch (Exception e) {
						e.printStackTrace();
					}
					break;
				case "-keystoreToXml":
					if (args.length != 2) {
						showUsage = true;
						break;
					}
					try {
						FileInputStream fis = new FileInputStream(args[1]);
						FileChannel channel = fis.getChannel();
						ByteBuffer bb = ByteBuffer.allocate((int) channel.size());
						channel.read(bb);
						byte[] contents = bb.array();
						channel.close();
						System.out.format("Base64 value of keystore file %s: %s\n", args[1], DatatypeConverter.printBase64Binary(contents));
					} catch (Exception e) {
						e.printStackTrace();
					}
					break;
				case "-encrypt":
					if (args.length != 3) {
						showUsage = true;
						break;
					}
					try {
						System.out.format("Clear text %s encrypted with %s encrypted value: %s\n", args[1], args[2], encrypt(args[1], args[2]));
					} catch (Exception e) {
						e.printStackTrace();
					}
					break;
				case "-decrypt":
					if (args.length != 3) {
						showUsage = true;
						break;
					}
					try {
						System.out.format("Encrypted text %s encrypted with %s clear text value: %s\n", args[1], args[2], decrypt(args[1], args[2]));
					} catch (Exception e) {
						e.printStackTrace();
					}

					break;
				case "-sign":
					if (args.length != 8) {
						showUsage = true;
						break;
					}
					try {
						DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
						dbf.setNamespaceAware(true);
						Document doc = dbf.newDocumentBuilder().parse(new FileInputStream(args[6]));
						KeyStore ks = KeyStore.getInstance("PKCS12");
						ks.load(new FileInputStream(args[1]), args[2].toCharArray());
						PrivateKey privateKey = (PrivateKey) ks.getKey(args[4], args[3].toCharArray());
						PublicKey publicKey = ks.getCertificate(args[4]).getPublicKey();
						KeyPair keyPair = new KeyPair(publicKey, privateKey);
						Document out = sign(doc, keyPair, Boolean.valueOf(args[5]));
						TransformerFactory transformFactory = TransformerFactory.newInstance();
						Transformer tform = transformFactory.newTransformer();
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						tform.transform(new DOMSource(out), new StreamResult(bos));
						Files.write(Paths.get(args[7]), bos.toByteArray());
						System.out.format("Signature of %s with key %s saved to %s\n", args[6], args[4], args[7]);
					} catch (Exception e) {
						e.printStackTrace();
					}
					break;
				case "-verify":
					if ((args.length != 3 && !Boolean.valueOf(args[1])) && args.length != 6) {
						showUsage = true;
						break;
					}
					try {
						DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
						dbf.setNamespaceAware(true);
						if (args.length == 3) {
							Document doc = dbf.newDocumentBuilder().parse(new FileInputStream(args[2]));
							System.out.format("Document %s is valid: %s\n", args[2], validate(doc, null));
						} else {
							Document doc = dbf.newDocumentBuilder().parse(new FileInputStream(args[5]));
							KeyStore ks = KeyStore.getInstance("PKCS12");
							ks.load(new FileInputStream(args[2]), args[3].toCharArray());
							PublicKey publicKey = ks.getCertificate(args[4]).getPublicKey();
							System.out.format("Document %s is valid: %s\n", args[5], validate(doc, publicKey));
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					break;
				}
			} else {
				showUsage = true;
			}
			if (showUsage) {
				System.out.println("Usage: ");
				System.out.println("\torg.apache.ode.runtime.exec.platform.NodeImpl$ClusterXMLDSig -genEncKey");
				System.out.println("\torg.apache.ode.runtime.exec.platform.NodeImpl$ClusterXMLDSig -keystoreToXml <keystore file>");
				System.out.println("\torg.apache.ode.runtime.exec.platform.NodeImpl$ClusterXMLDSig -encrypt <encKey> <cleartext>");
				System.out.println("\torg.apache.ode.runtime.exec.platform.NodeImpl$ClusterXMLDSig -decrypt <encKey> <cleartext>");
				System.out
						.println("\torg.apache.ode.runtime.exec.platform.NodeImpl$ClusterXMLDSig -sign <keystore> <keystorePass> <keyPass> <alias> <include keyInfo> <inXmlFile> <outXmlFile>");
				System.out
						.println("\torg.apache.ode.runtime.exec.platform.NodeImpl$ClusterXMLDSig -verify <useKeyInfo> <keystore> <keystorePass> <alias> <inXmlFile>");
			}
		}
	}

	public JAXBContext getJAXBContext(InputStream executable) throws JAXBException {
		try {
			return getJAXBContext(getInstructionSets(executable));
		} catch (PlatformException pe) {
			throw new JAXBException(pe);
		}
	}

	public JAXBContext getJAXBContext(List<QName> isets) throws JAXBException {
		Set<InstructionSet> isetSet = new HashSet<InstructionSet>();
		for (QName iset : isets) {
			InstructionSet is = getInstructionSets().get(iset);
			if (is == null) {
				throw new JAXBException(new PlatformException("Unknown instruction set " + iset.toString()));
			}
			isetSet.add(is);
		}
		return JAXBRuntimeUtil.executableJAXBContextByPath(isetSet);
	}

	public List<QName> getInstructionSets(InputStream executable) throws PlatformException {
		List<QName> isets = new ArrayList<QName>();
		try {
			XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(executable);
			reader.nextTag();// root
			reader.nextTag();// Instruction set, if present
			if ("instruction-sets".equals(reader.getLocalName())) {
				reader.nextTag();// first instruction set
				while ("instruction-set".equals(reader.getLocalName())) {
					String[] text = reader.getElementText().split(":");
					if (text.length == 2) {
						QName iset = new QName(reader.getNamespaceContext().getNamespaceURI(text[0]), text[1]);
						InstructionSet is = getInstructionSets().get(iset);
						if (is == null) {
							throw new PlatformException("Unknown instruction set " + iset.toString());
						}
						isets.add(iset);
					} else {
						throw new PlatformException("Unknown instruction set " + reader.getElementText());
					}
					reader.nextTag();
				}
			}
			executable.close();
			return isets;
		} catch (Exception e) {
			throw new PlatformException(e);
		}
	}

	public JAXBContext getJAXBContext(Executable executable) throws JAXBException {
		Set<InstructionSet> isetSet = new HashSet<InstructionSet>();
		InstructionSets isets = executable.getInstructionSets();
		if (isets != null) {
			for (QName iset : isets.getInstructionSet()) {
				InstructionSet is = getInstructionSets().get(iset);
				if (is == null) {
					throw new JAXBException(new PlatformException("Unknown instruction set " + iset.toString()));
				}
				isetSet.add(is);
			}
		}
		return JAXBRuntimeUtil.executableJAXBContextByPath(isetSet);
	}

	public List<QName> getInstructionSets(Executable executable) throws PlatformException {
		List<QName> isets = new ArrayList<QName>();
		InstructionSets eisets = executable.getInstructionSets();
		if (eisets != null) {
			for (QName iset : eisets.getInstructionSet()) {
				InstructionSet is = getInstructionSets().get(iset);
				if (is == null) {
					throw new PlatformException("Unknown instruction set " + iset.toString());
				}
				isets.add(iset);
			}
		}
		return isets;
	}

}
