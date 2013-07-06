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

import org.apache.ode.spi.exec.config.xml.ExecutionConfig;
import org.apache.ode.spi.exec.config.xml.Formula;
import org.apache.ode.spi.exec.config.xml.FormulaId;

public class ReactorImpl extends ExecutionConfig {
	private static final Logger log = Logger.getLogger(ReactorImpl.class.getName());

	ConcurrentHashMap<FormulaId, JAXBElement<? extends Formula>> formulaIdCache = new ConcurrentHashMap<>();
	ConcurrentHashMap<URI, JAXBElement<? extends Formula>> formulaURICache = new ConcurrentHashMap<>();

	public boolean marshalling = true;

	public <T extends Formula> T getFormula(FormulaId id) {
		JAXBElement formula = formulaIdCache.get(id);
		if (formula != null) {
			return (T) formula.getValue();
		}
		return null;
	}

	public <T extends Formula> T getFormula(URI uri) {
		JAXBElement formula = formulaURICache.get(uri);
		if (formula != null) {
			return (T) formula.getValue();
		}
		return null;
	}

	@Override
	public List<JAXBElement<? extends Formula>> getFormulas() {
		if (marshalling) {
			return super.formulas;
		}
		throw new IllegalAccessError("Formulas should only be manipulated through ExecutionRuntime SPI");
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

	// Invoked by Marshaller after it has created an formula of this object.
	boolean beforeMarshal(Marshaller marshaller) {
		System.out.format("beforeMarshal called\n");
		this.formulas = new ArrayList<>(formulaIdCache.values());
		Collections.sort(this.formulas, new Comparator<JAXBElement<? extends Formula>>() {
			public int compare(JAXBElement<? extends Formula> a, JAXBElement<? extends Formula> b) {
				return a.getValue().getId().id().compareTo(b.getValue().getId().id());
			}
		});
		this.marshalling = true;
		return true;
	}

	// Invoked by Marshaller after it has marshalled all properties of this object.
	void afterMarshal(Marshaller marshaller) {
		System.out.format("afterMarshal called\n");
		for (JAXBElement<? extends Formula> formula : super.formulas) {
			formulaIdCache.put(formula.getValue().getId(), formula);
			URI uri = formula.getValue().getUri();
			if (uri != null) {
				formulaURICache.put(uri, formula);
			}
		}
		this.formulas = null;
		this.marshalling = false;
	}

}
