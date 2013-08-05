package org.apache.ode.arch.gme;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import com.google.inject.AbstractModule;

public class RuntimeConfigModule extends AbstractModule {
	public static final String GME_NAMESPACE = "http://ode.apache.org/architecture/gme";
	public static final QName GME_ARCHITECTURE = new QName(GME_NAMESPACE,"GME");
	public static Logger log = Logger.getLogger(RuntimeConfigModule.class.getName());

	String configLocation = null;

	RuntimeConfigModule() {
		configLocation = "META-INF/gme.xml";
	}

	RuntimeConfigModule(String configName) {
		this.configLocation = configName;
	}

	protected void configure() {
		try {
			Runtime r = readConfig();
			bind(Runtime.class).toInstance(r);
		} catch (IOException ie) {
			log.log(Level.SEVERE, "", ie);
		}
	}

	public Runtime readConfig() throws IOException {
		if (configLocation == null) {
			throw new IOException("No configLocation specified");
		}
		ClassLoader currentCL = Thread.currentThread().getContextClassLoader();
		InputStream is = currentCL.getResourceAsStream(configLocation);
		if (is != null) {
			try {
				JAXBContext jc = JAXBContext.newInstance("org.apache.ode.arch.gme.xml:org.apache.ode.data.memory.repo.xml");
				Unmarshaller u = jc.createUnmarshaller();
				JAXBElement<Runtime> element = (JAXBElement<Runtime>) u.unmarshal(is);
				return element.getValue();

			} catch (JAXBException je) {
				throw new IOException(je);
			}
		} else {
			throw new IOException(String.format("Unable to locate config file %s on classpath\n", configLocation));
		}

	}

}