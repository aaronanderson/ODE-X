package org.apache.ode.spi.compiler;


public interface InlineSource extends Source {
	
	String inlineContentType();
	
	Location start();
	
	Location end();
}