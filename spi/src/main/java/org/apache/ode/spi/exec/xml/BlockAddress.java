package org.apache.ode.spi.exec.xml;

public class BlockAddress {

	private String blcAddress;

	
	public String getInsAddress() {
		return blcAddress;
	}

	public void setBlcAddress(String blcAddress) {
		this.blcAddress = blcAddress;
	}
	

	public BlockAddress(String blcAddress) {
		this.blcAddress = blcAddress;
	}

	static String printAddress(BlockAddress address) {
		return address.blcAddress;
	}

	static BlockAddress parseAddress(String insAddress) {
		return new BlockAddress(insAddress);
	}
}
