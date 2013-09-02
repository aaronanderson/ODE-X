package org.apache.ode.runtime.memory.node;

import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.namespace.QName;

import org.apache.ode.runtime.core.node.NodeBase;

@Singleton
public class NodeImpl extends NodeBase {

	private static final Logger log = Logger.getLogger(NodeImpl.class.getName());

	

	@Inject
	@Architecture
	QName architecture;


	@Override
	public QName architecture() {
		return architecture;
	}

	

}
