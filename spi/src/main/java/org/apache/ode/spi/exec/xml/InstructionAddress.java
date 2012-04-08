package org.apache.ode.spi.exec.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlID;
@XmlAccessorType(XmlAccessType.FIELD)
public class InstructionAddress {
	
	@XmlID
	 @XmlAttribute
	private String insAddress;

	
	public String getInsAddress() {
		return insAddress;
	}

	public void setInsAddress(String insAddress) {
		this.insAddress = insAddress;
	}
	

	public InstructionAddress(String insAddress) {
		this.insAddress = insAddress;
	}

	static String printAddress(InstructionAddress address) {
		return address.insAddress;
	}

	static InstructionAddress parseAddress(String insAddress) {
		return new InstructionAddress(insAddress);
	}
}
