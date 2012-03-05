package org.apache.ode.spi.compiler;

public class Location {
	final int line;
	final int column;
	final int offset;
	
	public Location(javax.xml.stream.Location location){
		this.line = location.getLineNumber();
		this.column = location.getColumnNumber();
		this.offset = location.getCharacterOffset();
	}

	public Location(int line, int column, int offset) {
		this.line = line;
		this.column = column;
		this.offset = offset;
	}

	public int getLine() {
		return line;
	}

	public int getColumn() {
		return column;
	}

	public int getOffset() {
		return offset;
	}

}