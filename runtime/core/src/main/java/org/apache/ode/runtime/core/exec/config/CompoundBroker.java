package org.apache.ode.runtime.core.exec.config;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.ode.spi.exec.config.xml.Element;
import org.apache.ode.spi.exec.config.xml.BondId;
import org.apache.ode.spi.exec.config.xml.Formula;

public class CompoundBroker extends Formula {

	private static final Logger log = Logger.getLogger(CompoundBroker.class.getName());

	ConcurrentHashMap<BondId, JAXBElement<? extends Element>> ElementIdCache = new ConcurrentHashMap<>();
	ConcurrentHashMap<URI, JAXBElement<? extends Element>> ElementURICache = new ConcurrentHashMap<>();

	public boolean marshalling = true;

	public <T extends Element> T getElement(BondId id) {
		JAXBElement Element = ElementIdCache.get(id);
		if (Element != null) {
			return (T) Element.getValue();
		}
		return null;
	}

	public <T extends Element> T getElement(URI uri) {
		JAXBElement Element = ElementURICache.get(uri);
		if (Element != null) {
			return (T) Element.getValue();
		}
		return null;
	}

	@Override
	public List<JAXBElement<? extends Element>> getElements() {
		if (marshalling) {
			return super.elements;
		}
		throw new IllegalAccessError("Elements should only be manipulated through ExecutionRuntime SPI");
	}

	void beforeUnmarshal(Unmarshaller unmarshaller, Object parent) {
		System.out.format("beforeUnmarshal called %s\n", parent);
		this.marshalling = true;

	}

	//This method is called after all the properties (except IDREF) are unmarshalled for this object, 
	//but before this object is set to the parent object.
	void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
		System.out.format("afterUnmarshal called %s\n", parent);
		this.marshalling = false;
	}

	// Invoked by Marshaller after it has created an instance of this object.
	boolean beforeMarshal(Marshaller marshaller) {
		System.out.format("beforeMarshal called\n");
		this.elements = new ArrayList<>(ElementIdCache.values());
		Collections.sort(this.elements, new Comparator<JAXBElement<? extends Element>>() {
			public int compare(JAXBElement<? extends Element> a, JAXBElement<? extends Element> b) {
				return a.getValue().getId().id().compareTo(b.getValue().getId().id());
			}
		});
		this.marshalling = true;
		return true;
	}

	// Invoked by Marshaller after it has marshalled all properties of this object.
	void afterMarshal(Marshaller marshaller) {
		System.out.format("afterMarshal called\n");
		for (JAXBElement<? extends Element> Element : super.elements) {
			ElementIdCache.put(Element.getValue().getId(), Element);
			ElementURICache.put(Element.getValue().getUri(), Element);

		}
		this.elements = null;
		this.marshalling = false;
	}

}
