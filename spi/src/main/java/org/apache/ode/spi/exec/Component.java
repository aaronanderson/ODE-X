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
package org.apache.ode.spi.exec;

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

	//public QName name();

	@Retention(RUNTIME)
	@Target(METHOD)
	public @interface ExecutableSets {

	}

	//public List<ExecutableSet> executableSets();

	public class ExecutableSet {
		final QName name;
		final String jaxbExecPath;
		final Class<?> jaxbExecFactory;
		final QName execContextSetName;
		final QName eventSetName;
		final QName programSetName;

		public ExecutableSet(QName name, String jaxbExecPath, Class<?> jaxbExecFactory, QName execContextSetName, QName eventSetName, QName programSetName) {
			if (name == null || ((jaxbExecPath == null && jaxbExecFactory == null))) {
				throw new NullPointerException("name and path or class required");
			}
			this.name = name;
			this.jaxbExecPath = jaxbExecPath;
			this.jaxbExecFactory = jaxbExecFactory;
			this.execContextSetName = execContextSetName;
			this.eventSetName = eventSetName;
			this.programSetName = programSetName;

		}

		public QName getName() {
			return name;
		}

		public String getJAXBExecPath() {
			return jaxbExecPath;
		}

		public Class<?> getJAXBExecFactory() {
			return jaxbExecFactory;
		}

		public QName getExecContextSetName() {
			return execContextSetName;
		}

		public QName getEventSetName() {
			return eventSetName;
		}

		public QName getProgramSetName() {
			return programSetName;
		}

	}

	@Retention(RUNTIME)
	@Target(METHOD)
	public @interface ExecutionContextSets {

	}

	//public List<ExecutionContextSet> executionContextSets();

	public class ExecutionContextSet {
		final QName name;
		final String jaxbExecContextPath;
		final Class<?> jaxbExecContextFactory;

		public ExecutionContextSet(QName name, String jaxbExecContextPath, Class<?> jaxbExecContextFactory) {
			if (name == null || ((jaxbExecContextPath == null && jaxbExecContextFactory == null))) {
				throw new NullPointerException("name and path or class required");
			}
			this.name = name;
			this.jaxbExecContextPath = jaxbExecContextPath;
			this.jaxbExecContextFactory = jaxbExecContextFactory;
		}

		public QName getName() {
			return name;
		}

		public String getJAXBExecContextPath() {
			return jaxbExecContextPath;
		}

		public Class<?> getJAXBExecContextFactory() {
			return jaxbExecContextFactory;
		}
	}

	@Retention(RUNTIME)
	@Target(METHOD)
	public @interface EventSets {

	}

	//public List<EventSet> eventSets();

	public class EventSet {
		final QName name;
		final String jaxbEventPath;
		final Class<?> jaxbEventFactory;
		final Map<String, Class<? extends Event>> eventTypes;

		private static final Logger log = Logger.getLogger(EventSet.class.getName());

		public EventSet(QName name, String jaxbEventPath, Class<?> jaxbEventFactory) {
			if (name == null || ((jaxbEventPath == null && jaxbEventFactory == null))) {
				throw new NullPointerException("name and path or class required");
			}
			this.name = name;
			this.jaxbEventPath = jaxbEventPath;
			this.jaxbEventFactory = jaxbEventFactory;
			this.eventTypes = buildEvents(jaxbEventPath, jaxbEventFactory);
		}

		public QName getName() {
			return name;
		}

		public String getJAXBEventPath() {
			return jaxbEventPath;
		}

		public Class<?> getJAXBEventFactory() {
			return jaxbEventFactory;
		}

		public Map<String, Class<? extends Event>> getEventTypes() {
			return eventTypes;
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
	public @interface ProgramSets {

	}

	//public List<ProgramSet> programSets();

	public class ExecutionConfigSet {
		final QName name;
		final String jaxbProgramPath;
		final Class<?> jaxbProgramFactory;
		final QName eventSetName;

		public ExecutionConfigSet(QName name, String jaxbProgramPath, Class<?> jaxbProgramFactory, QName eventSetName) {

			if (name == null || ((jaxbProgramPath == null && jaxbProgramFactory == null))) {
				throw new NullPointerException("name and path or class required");
			}
			this.name = name;
			this.jaxbProgramPath = jaxbProgramPath;
			this.jaxbProgramFactory = jaxbProgramFactory;
			this.eventSetName = eventSetName;
		}

		public QName getName() {
			return name;
		}

		public QName getEventSetName() {
			return eventSetName;
		}

		public String getJAXBProgramPath() {
			return jaxbProgramPath;
		}

		public Class<?> getJAXBProgramFactory() {
			return jaxbProgramFactory;
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
	public @interface Online {

	}

	//public void online() throws PlatformException;
	@Retention(RUNTIME)
	@Target(METHOD)
	public @interface Offline {

	}
	//public void offline() throws PlatformException;

}
