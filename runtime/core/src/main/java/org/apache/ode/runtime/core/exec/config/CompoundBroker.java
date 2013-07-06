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

import org.apache.ode.spi.exec.config.xml.BondPoint;
import org.apache.ode.spi.exec.config.xml.BondPointId;
import org.apache.ode.spi.exec.config.xml.Formula;

public class CompoundBroker extends Formula {

	private static final Logger log = Logger.getLogger(CompoundBroker.class.getName());

	ConcurrentHashMap<BondPointId, JAXBElement<? extends BondPoint>> bondPointIdCache = new ConcurrentHashMap<>();
	ConcurrentHashMap<URI, JAXBElement<? extends BondPoint>> bondPointURICache = new ConcurrentHashMap<>();

	public boolean marshalling = true;

	public <T extends BondPoint> T getBondPoint(BondPointId id) {
		JAXBElement bondPoint = bondPointIdCache.get(id);
		if (bondPoint != null) {
			return (T) bondPoint.getValue();
		}
		return null;
	}

	public <T extends BondPoint> T getBondPoint(URI uri) {
		JAXBElement bondPoint = bondPointURICache.get(uri);
		if (bondPoint != null) {
			return (T) bondPoint.getValue();
		}
		return null;
	}

	@Override
	public List<JAXBElement<? extends BondPoint>> getBondPoints() {
		if (marshalling) {
			return super.bondPoints;
		}
		throw new IllegalAccessError("BondPoints should only be manipulated through ExecutionRuntime SPI");
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
		this.bondPoints = new ArrayList<>(bondPointIdCache.values());
		Collections.sort(this.bondPoints, new Comparator<JAXBElement<? extends BondPoint>>() {
			public int compare(JAXBElement<? extends BondPoint> a, JAXBElement<? extends BondPoint> b) {
				return a.getValue().getId().id().compareTo(b.getValue().getId().id());
			}
		});
		this.marshalling = true;
		return true;
	}

	// Invoked by Marshaller after it has marshalled all properties of this object.
	void afterMarshal(Marshaller marshaller) {
		System.out.format("afterMarshal called\n");
		for (JAXBElement<? extends BondPoint> bondPoint : super.bondPoints) {
			bondPointIdCache.put(bondPoint.getValue().getId(), bondPoint);
			bondPointURICache.put(bondPoint.getValue().getUri(), bondPoint);

		}
		this.bondPoints = null;
		this.marshalling = false;
	}

}
