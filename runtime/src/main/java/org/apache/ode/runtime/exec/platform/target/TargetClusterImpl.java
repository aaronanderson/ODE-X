package org.apache.ode.runtime.exec.platform.target;

import static org.apache.ode.runtime.exec.platform.target.TargetClusterImpl.TYPE;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.apache.ode.spi.exec.target.TargetCluster;

@Entity
@DiscriminatorValue(TYPE)
public class TargetClusterImpl extends TargetImpl implements TargetCluster, Serializable {
	public static final String TYPE = "CLUSTER";

	//To avoid JPA warnings about no properties
	@Column(name="DESCRIPTION")
	String description;
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public TargetClusterImpl() {
		this.type = TYPE;
	}

	@Override
	public String clusterId() {
		return id;
	}

	@Override
	public String[] memberNodeIds() {
		return nodeIds();
	}
	
	public static TargetPK getKey(String clusterId) {
		TargetPK pk = new TargetPK();
		pk.id = clusterId;
		pk.type = TYPE;
		return pk;
	}

}