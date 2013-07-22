package org.apache.ode.runtime.core.node;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.namespace.QName;

import org.apache.ode.spi.di.ComponentAnnotationScanner.ComponentModel;
import org.apache.ode.spi.di.ComponentAnnotationScanner.Components;
import org.apache.ode.spi.di.DIContainer;
import org.apache.ode.spi.di.NodeStatusAnnotationScanner.NodeStatusModel;
import org.apache.ode.spi.di.NodeStatusAnnotationScanner.NodeStatuses;
import org.apache.ode.spi.exec.Component.EventSet;
import org.apache.ode.spi.exec.Component.ExecutableSet;
import org.apache.ode.spi.exec.Component.ExecutionConfigSet;
import org.apache.ode.spi.exec.Component.ExecutionContextSet;
import org.apache.ode.spi.exec.Node;
import org.apache.ode.spi.exec.PlatformException;
import org.apache.ode.spi.exec.bond.Reactor;

@Singleton
public abstract class NodeBase implements Node {

	private static final Logger log = Logger.getLogger(NodeBase.class.getName());

	@Inject
	@Components
	Map<Class<?>, ComponentModel> componentModels;

	@Inject
	@NodeStatuses
	Map<Class<?>, NodeStatusModel> nodeStatusModels;

	@Inject
	private DIContainer container;

	private Status status = Status.OFFLINE;
	private List<Object> components = new ArrayList<>();
	private List<Object> listeners = new ArrayList<>();
	private Lock stateLock = new ReentrantLock();
	private Set<QName> componentNames = new HashSet<>();
	private Map<QName, EventSet> eventSets = new HashMap<>();
	private Map<QName, ExecutableSet> execSets = new HashMap<>();
	private Map<QName, ExecutionContextSet> execCtxSets = new HashMap<>();
	private Map<QName, ExecutionConfigSet> execCfgSets = new HashMap<>();

	@Override
	public Set<QName> getComponentNames() throws PlatformException {
		if (status != Status.ONLINE) {
			throw new PlatformException("Node offline");
		}
		return componentNames;

	}

	@Override
	public Map<QName, EventSet> eventSets() throws PlatformException {
		if (status != Status.ONLINE) {
			throw new PlatformException("Node offline");
		}
		return eventSets;
	}

	@Override
	public Map<QName, ExecutableSet> executableSets() throws PlatformException {
		if (status != Status.ONLINE) {
			throw new PlatformException("Node offline");
		}
		return execSets;
	}

	@Override
	public Map<QName, ExecutionConfigSet> executionConfigSets() throws PlatformException {
		if (status != Status.ONLINE) {
			throw new PlatformException("Node offline");
		}
		return execCfgSets;
	}

	@Override
	public Map<QName, ExecutionContextSet> executionContextSets() throws PlatformException {
		if (status != Status.ONLINE) {
			throw new PlatformException("Node offline");
		}
		return execCtxSets;
	}

	@Override
	public Status status() {
		return status;
	}

	protected void clear() {
		components.clear();
		listeners.clear();
		eventSets.clear();
		execSets.clear();
		execCtxSets.clear();
		execCfgSets.clear();
	}

	@Override
	public void online() throws PlatformException {
		stateLock.lock();
		try {
			if (Status.ONLINE == status) {
				throw new PlatformException("Node is allready online");
			}
			clear();
			PlatformException se = new PlatformException("Online Exceptions");
			for (ComponentModel componentModel : componentModels.values()) {
				Object component = container.getInstance(componentModel.getTargetClass());
				if (component == null) {
					se.addSuppressed(new PlatformException(String.format("unable to obtain component instance %s", componentModel.getTargetClass())));
				} else {
					try {
						if (componentModel.getEventSets() != null) {
							for (EventSet e : (Collection<? extends EventSet>) componentModel.getEventSets().invoke(component)) {
								eventSets.put(e.getName(), e);
							}
						}
						if (componentModel.getExecutableSets() != null) {
							for (ExecutableSet e : (Collection<? extends ExecutableSet>) componentModel.getExecutableSets().invoke(component)) {
								execSets.put(e.getName(), e);
							}
						}
						if (componentModel.getExecutionContextSets() != null) {
							for (ExecutionContextSet e : (Collection<? extends ExecutionContextSet>) componentModel.getExecutionContextSets().invoke(component)) {
								execCtxSets.put(e.getName(), e);
							}
						}
						if (componentModel.getExecutionConfigSets() != null) {
							for (ExecutionConfigSet e : (Collection<? extends ExecutionConfigSet>) componentModel.getExecutionConfigSets().invoke(component)) {
								execCfgSets.put(e.getName(), e);
							}
						}
						try {
							if (componentModel.getOnline() != null) {
								componentModel.getOnline().invoke(component);
							}
						} catch (PlatformException pe) {
							se.addSuppressed(pe);
						}
						components.add(component);
					} catch (Throwable e) {
						clear();
						throw new PlatformException(e);
					}
				}
			}
			for (NodeStatusModel statusModel : nodeStatusModels.values()) {
				Object listener = container.getInstance(statusModel.getTargetClass());
				if (listener == null) {
					se.addSuppressed(new PlatformException(String.format("unable to obtain NodeStatus instance %s", statusModel.getTargetClass())));
				} else {
					try {
						try {
							if (statusModel.getOnline() != null) {
								statusModel.getOnline().invoke(listener);
							}
						} catch (PlatformException pe) {
							se.addSuppressed(pe);
						}
						listeners.add(listener);
					} catch (Throwable e) {
						clear();
						throw new PlatformException(e);
					}
				}
			}			
			if (se.getSuppressed().length > 0) {
				throw se;
			}
		} finally {
			status = Status.ONLINE;
			stateLock.unlock();
		}

	}

	@Override
	public void offline() throws PlatformException {
		stateLock.lock();
		try {
			if (Status.OFFLINE == status) {
				throw new PlatformException("Node is allready offline");
			}			
			PlatformException se = new PlatformException("Online Exceptions");
			for (Object component : components) {
				ComponentModel componentModel = componentModels.get(component.getClass());
				try {
					try {
						if (componentModel.getOffline() != null) {
							componentModel.getOffline().invoke(component);
						}
					} catch (PlatformException pe) {
						se.addSuppressed(pe);
					}
				} catch (Throwable e) {
					throw new PlatformException(e);
				}

			}
			for (Object listener : listeners) {
				NodeStatusModel statusModel = nodeStatusModels.get(listener.getClass());
				try {
					try {
						if (statusModel.getOffline() != null) {
							statusModel.getOffline().invoke(listener);
						}
					} catch (PlatformException pe) {
						se.addSuppressed(pe);
					}
				} catch (Throwable e) {
					throw new PlatformException(e);
				}
			}
			if (se.getSuppressed().length > 0) {
				throw se;
			}
		} finally {
			clear();
			status = Status.OFFLINE;
			stateLock.unlock();
		}

	}

	@Override
	public Reactor reactor(URI execution) throws PlatformException {
		// TODO Auto-generated method stub
		return null;
	}

}
