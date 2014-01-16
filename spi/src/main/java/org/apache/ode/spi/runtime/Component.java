/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ode.spi.runtime;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.ode.spi.event.xml.Event;

@Retention(RUNTIME)
@Target(TYPE)
public @interface Component {

	public String value() default "";
	
	public Depend[] depends() default {};

	//public QName name();
	
	
	@Retention(RUNTIME)
	@Target(TYPE)
	public @interface Depend {
		public String value() default "";
	}

	@Retention(RUNTIME)
	@Target(METHOD)
	public @interface ExecutableSets {

	}

	//public List<ExecutableSet> executableSets();

	public class JAXBSet {
		final QName name;
		final String jaxbPath;
		final Class<?> jaxbFactory;

		public JAXBSet(QName name, String jaxbPath, Class<?> jaxbFactory) {
			if (name == null || ((jaxbPath == null && jaxbFactory == null))) {
				throw new NullPointerException("name and path or class required");
			}
			this.name = name;
			this.jaxbPath = jaxbPath;
			this.jaxbFactory = jaxbFactory;
		}

		public QName getName() {
			return name;
		}

		public String getJAXBPath() {
			return jaxbPath;
		}

		public Class<?> getJAXBFactory() {
			return jaxbFactory;
		}

	}

	public class ExecutableSet extends JAXBSet {

		public ExecutableSet(QName name, String jaxbExecPath, Class<?> jaxbExecFactory) {
			super(name, jaxbExecPath, jaxbExecFactory);

		}

	}

	@Retention(RUNTIME)
	@Target(METHOD)
	public @interface ExecutionContextSets {

	}

	//public List<ExecutionContextSet> executionContextSets();

	public class ExecutionContextSet extends JAXBSet {

		public ExecutionContextSet(QName name, String jaxbExecContextPath, Class<?> jaxbExecContextFactory) {
			super(name, jaxbExecContextPath, jaxbExecContextFactory);
		}
	}

	@Retention(RUNTIME)
	@Target(METHOD)
	public @interface EventSets {

	}

	//public List<EventSet> eventSets();

	public class EventSet extends JAXBSet {

		private static final Logger log = Logger.getLogger(EventSet.class.getName());

		public EventSet(QName name, String jaxbEventPath, Class<?> jaxbEventFactory) {
			super(name, jaxbEventPath, jaxbEventFactory);
		}

		public static Map<String, Class<? extends Event>> buildEvents(String jaxbEventPath, Class<?> jaxbEventFactory) {
			try {
				if (jaxbEventFactory == null) {
					if (jaxbEventPath == null) {
						return null;
					}
					jaxbEventFactory = Class.forName(jaxbEventPath + ".ObjectFactory");
				}
				Map<String, Class<? extends Event>> eventTypes = new HashMap<String, Class<? extends Event>>();
				for (Method m : jaxbEventFactory.getMethods()) {
					if (m.getName().startsWith("create")) {
						if (Event.class.isAssignableFrom(m.getReturnType())) {
							try {
								Field type = m.getReturnType().getField("type");
								eventTypes.put((String) type.get(null), (Class<? extends Event>) m.getReturnType());
							} catch (NoSuchFieldException nsfe) {
								log.log(Level.WARNING, String.format("Event class %s is does not have fixed type attribute field", m.getReturnType().getName()));
							}
						}
					}
				}
				return Collections.unmodifiableMap(eventTypes);
			} catch (Exception e) {
				log.log(Level.SEVERE, "", e);
			}

			return null;
		}

	}

	@Retention(RUNTIME)
	@Target(METHOD)
	public @interface ExecutionConfigSets {

	}

	//public List<ProgramSet> programSets();

	public class ExecutionConfigSet extends JAXBSet {

		public ExecutionConfigSet(QName name, String jaxbExecConfigPath, Class<?> jaxbExecConfigFactory) {

			super(name, jaxbExecConfigPath, jaxbExecConfigFactory);
		}

		public QName getName() {
			return name;
		}

	}

	@Retention(RUNTIME)
	@Target(METHOD)
	public @interface Tasks {

	}

	//public List<TaskDefinition> tasks();

	@Retention(RUNTIME)
	@Target(METHOD)
	public @interface TaskActions {

	}

	//public List<TaskActionDefinition> actions();

	@Retention(RUNTIME)
	@Target(METHOD)
	public @interface Start {

	}
	
	@Retention(RUNTIME)
	@Target(METHOD)
	public @interface Online {

	}

	@Retention(RUNTIME)
	@Target(METHOD)
	public @interface Stop {

	}
	
	//public void online() throws PlatformException;
	@Retention(RUNTIME)
	@Target(METHOD)
	public @interface Offline {

	}
	//public void offline() throws PlatformException;

}
