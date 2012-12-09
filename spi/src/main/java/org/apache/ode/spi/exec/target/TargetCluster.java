package org.apache.ode.spi.exec.target;

public interface TargetCluster extends Target {
	String clusterId();
	
	String[] memberNodeIds();
}