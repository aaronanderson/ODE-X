package org.apache.ode.runtime.memory.node;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.xml.namespace.QName;

import org.apache.ode.runtime.core.node.NodeBase;
import org.apache.ode.spi.di.ComponentAnnotationScanner.ComponentModel;
import org.apache.ode.spi.di.ComponentAnnotationScanner.Components;
import org.apache.ode.spi.di.DIContainer;
import org.apache.ode.spi.exec.Component.EventSet;
import org.apache.ode.spi.exec.Component.ExecutableSet;
import org.apache.ode.spi.exec.Component.ExecutionConfigSet;
import org.apache.ode.spi.exec.Component.ExecutionContextSet;
import org.apache.ode.spi.exec.Node;
import org.apache.ode.spi.exec.PlatformException;
import org.apache.ode.spi.exec.bond.Reactor;

@Singleton
public class NodeImpl extends NodeBase {

	private static final Logger log = Logger.getLogger(NodeImpl.class.getName());

	@Override
	public QName architecture() {
		// TODO Auto-generated method stub
		return null;
	}


}
