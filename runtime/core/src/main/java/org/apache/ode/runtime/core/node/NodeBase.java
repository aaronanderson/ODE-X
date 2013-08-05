package org.apache.ode.runtime.core.node;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
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
import javax.inject.Qualifier;
import javax.inject.Singleton;
import javax.xml.namespace.QName;

import org.apache.ode.spi.bond.Reactor;
import org.apache.ode.spi.di.ComponentAnnotationScanner.ComponentModel;
import org.apache.ode.spi.di.ComponentAnnotationScanner.Components;
import org.apache.ode.spi.di.DIContainer;
import org.apache.ode.spi.di.NodeStatusAnnotationScanner.NodeStatusModel;
import org.apache.ode.spi.di.NodeStatusAnnotationScanner.NodeStatuses;
import org.apache.ode.spi.runtime.Node;
import org.apache.ode.spi.runtime.PlatformException;
import org.apache.ode.spi.runtime.Component.EventSet;
import org.apache.ode.spi.runtime.Component.ExecutableSet;
import org.apache.ode.spi.runtime.Component.ExecutionConfigSet;
import org.apache.ode.spi.runtime.Component.ExecutionContextSet;

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

	@Qualifier
	@Retention(RUNTIME)
	@Target(FIELD)
	public @interface Architecture {

	}

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

	protected void notifyComponents(Status status, PlatformException se) throws PlatformException {
		if (status == Status.START) {
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
							if (componentModel.getStart() != null) {
								componentModel.getStart().invoke(component);
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
		} else {
			for (Object component : components) {
				ComponentModel componentModel = componentModels.get(component.getClass());
				try {
					try {
						switch (status) {
						case START:
							break;
						case ONLINE:
							if (componentModel.getOnline() != null) {
								componentModel.getOnline().invoke(component);
							}
							break;
						case STOP:
							if (componentModel.getStop() != null) {
								componentModel.getStop().invoke(component);
							}
							break;
						case OFFLINE:
							if (componentModel.getOffline() != null) {
								componentModel.getOffline().invoke(component);
							}
							break;
						}
					} catch (PlatformException pe) {
						se.addSuppressed(pe);
					}
				} catch (Throwable e) {
					throw new PlatformException(e);
				}
			}

		}

	}

	protected void notifyNodeStatusListeners(Status status, PlatformException se) throws PlatformException {

		if (status == Status.START) {
			for (NodeStatusModel statusModel : nodeStatusModels.values()) {
				Object listener = container.getInstance(statusModel.getTargetClass());
				if (listener == null) {
					se.addSuppressed(new PlatformException(String.format("unable to obtain NodeStatus instance %s", statusModel.getTargetClass())));
				} else {
					try {
						try {
							if (statusModel.getStart() != null) {
								statusModel.getStart().invoke(listener);
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
		} else {
			for (Object listener : listeners) {
				NodeStatusModel statusModel = nodeStatusModels.get(listener.getClass());
				try {
					try {
						switch (status) {
						case START:
							break;
						case ONLINE:
							if (statusModel.getOnline() != null) {
								statusModel.getOnline().invoke(listener);
							}
							break;
						case STOP:
							if (statusModel.getStop() != null) {
								statusModel.getStop().invoke(listener);
							}
							break;
						case OFFLINE:
							if (statusModel.getOffline() != null) {
								statusModel.getOffline().invoke(listener);
							}
							break;
						}
					} catch (PlatformException pe) {
						se.addSuppressed(pe);
					}
				} catch (Throwable e) {
					throw new PlatformException(e);
				}
			}

		}

	}

	protected void clear() {
		components.clear();
		listeners.clear();
		eventSets.clear();
		execSets.clear();
		execCtxSets.clear();
		execCfgSets.clear();
	}

	//protected abstract void implStart() throws PlatformException;

	//protected abstract void implOnline() throws PlatformException;

	@Override
	public void online() throws PlatformException {
		stateLock.lock();
		try {
			if (Status.START == status || Status.ONLINE == status) {
				throw new PlatformException("Node is allready " + status);
			}
			clear();

			status = Status.START;
			PlatformException se = new PlatformException("Start Exceptions");
			/*try {
				implStart();
			} catch (PlatformException pe) {
				se.addSuppressed(pe);
			}*/
			notifyComponents(Status.START, se);
			notifyNodeStatusListeners(Status.START, se);
			if (se.getSuppressed().length > 0) {
				status = Status.OFFLINE;
				throw se;
			}

			se = new PlatformException("Online Exceptions");
			/*try {
				implOnline();
			} catch (PlatformException pe) {
				se.addSuppressed(pe);
			}*/
			notifyComponents(Status.ONLINE, se);
			notifyNodeStatusListeners(Status.ONLINE, se);
			if (se.getSuppressed().length > 0) {
				status = Status.OFFLINE;
				throw se;
			}
		} finally {
			status = Status.ONLINE;
			stateLock.unlock();
		}

	}

	//protected abstract void implStop() throws PlatformException;

	//protected abstract void implOffline() throws PlatformException;

	@Override
	public void offline() throws PlatformException {
		stateLock.lock();
		try {
			if (Status.STOP == status || Status.OFFLINE == status) {
				throw new PlatformException("Node is allready " + status);
			}

			status = Status.STOP;
			PlatformException se = new PlatformException("Stop Exceptions");

			notifyNodeStatusListeners(Status.STOP, se);
			notifyComponents(Status.STOP, se);
			/*try {
				implStop();
			} catch (PlatformException pe) {
				se.addSuppressed(pe);
			}*/
			if (se.getSuppressed().length > 0) {
				status = Status.ONLINE;
				throw se;
			}

			se = new PlatformException("Offline Exceptions");
			notifyNodeStatusListeners(Status.OFFLINE, se);
			notifyComponents(Status.OFFLINE, se);
			/*try {
				implOffline();
			} catch (PlatformException pe) {
				se.addSuppressed(pe);
			}*/
			if (se.getSuppressed().length > 0) {
				status = Status.OFFLINE;
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
