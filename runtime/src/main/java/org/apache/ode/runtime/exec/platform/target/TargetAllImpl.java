package org.apache.ode.runtime.exec.platform.target;

import static org.apache.ode.runtime.exec.platform.target.TargetAllImpl.TYPE;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.apache.ode.spi.exec.target.TargetAll;

@Entity
@DiscriminatorValue(TYPE)
public class TargetAllImpl extends TargetImpl implements TargetAll, Serializable {
	public static final String TYPE = "ALL";

	//To avoid JPA warnings about no properties
	@Column(name="DESCRIPTION")
	String description;
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public TargetAllImpl() {
		this.id = TYPE;
		this.type = TYPE;
	}

	public static TargetPK getKey() {
		TargetPK pk = new TargetPK();
		pk.id = TYPE;
		pk.type = TYPE;
		return pk;
	}

}