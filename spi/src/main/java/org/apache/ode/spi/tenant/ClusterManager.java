package org.apache.ode.spi.tenant;

public interface ClusterManager {

	void activate(boolean value);

	void baselineTopology(long version);

}
