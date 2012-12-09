package org.apache.ode.runtime.exec.platform.target;

import static org.apache.ode.runtime.exec.platform.target.TargetNodeImpl.TYPE;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.apache.ode.spi.exec.target.TargetNode;

@Entity
@DiscriminatorValue(TYPE)
public class TargetNodeImpl extends TargetImpl implements TargetNode, Serializable {
	public static final String TYPE = "NODE";
	//To avoid JPA warnings about no properties
	@Column(name="DESCRIPTION")
	String description;
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}


	public TargetNodeImpl() {
		this.type = TYPE;
	}

	@Override
	public String nodeId() {
		return id;
	}

	public static TargetPK getKey(String nodeId) {
		TargetPK pk = new TargetPK();
		pk.id = nodeId;
		pk.type = TYPE;
		return pk;
	}
}