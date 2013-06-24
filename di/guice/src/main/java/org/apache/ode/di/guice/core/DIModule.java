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
package org.apache.ode.di.guice.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.ode.spi.di.AnnotationScanner;
import org.apache.ode.spi.di.OperationAnnotationScanner;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

public class DIModule extends AbstractModule {

	public static Logger log = Logger.getLogger(DIModule.class.getName());

	protected GuiceAnnotationProcessor[] annotationProcessors;
	protected Map<Class<?>, Object> models = new HashMap<Class<?>, Object>();

	public DIModule(GuiceAnnotationProcessor... annotationProcessors) {
		this.annotationProcessors = annotationProcessors;
	}

	protected void configure() {
		bindListener(Matchers.any(), new AnnotationProcessorListener());
	}

	protected class AnnotationProcessorListener implements TypeListener {

		public <T> void hear(TypeLiteral<T> typeLiteral, TypeEncounter<T> typeEncounter) {
			for (GuiceAnnotationProcessor p : annotationProcessors) {
				p.process(models, typeLiteral, typeEncounter);

			}
			/*for (Field field : typeLiteral.getRawType().getDeclaredFields()) {
				if (field.getType() == EntityManager.class || field.getType() == EntityManagerFactory.class) {
					String unitName = null;
					if (field.isAnnotationPresent(PersistenceContext.class)) {
						PersistenceContext pc = field.getAnnotation(PersistenceContext.class);
						unitName = pc.unitName();
					} else if (field.isAnnotationPresent(PersistenceUnit.class)) {
						PersistenceUnit pc = field.getAnnotation(PersistenceUnit.class);
						unitName = pc.unitName();
					}
					if (unitName != null) {
						EntityManagerFactory emf = pus.get(unitName);
						if (emf == null) {
							emf = Persistence.createEntityManagerFactory(unitName);
							pus.put(unitName, emf);
						}
						typeEncounter.register(new JPASessionMembersInjector(field, emf));
					}

				}

			}*/
		}
	}

	/*public static class JPASessionMembersInjector<T> implements MembersInjector<T> {
		private final Field field;
		private final EntityManagerFactory emf;

		JPASessionMembersInjector(Field field, EntityManagerFactory emf) {
			this.field = field;
			this.emf = emf;
			field.setAccessible(true);
		}

		public void injectMembers(T t) {
			try {
				if (field.getType() == EntityManager.class) {
					field.set(t, emf.createEntityManager());
					//TODO How will the entity manager.close() method be invoked on disposal?
				} else if (field.getType() == EntityManagerFactory.class) {
					field.set(t, emf);
				} else {
					throw new Exception(String.format("Unexpected type %s", field.getType().getName()));
				}
			} catch (Throwable e) {
				log.log(Level.SEVERE, "", e);
				throw new RuntimeException(e);
			}
		}
	}*/

	public static interface GuiceAnnotationProcessor {

		public <T> void process(Map<Class<?>, Object> models, TypeLiteral<T> typeLiteral, TypeEncounter<T> typeEncounter);

	}

	public static class OperationAnnotationProcessor implements GuiceAnnotationProcessor {
		OperationAnnotationScanner scanner = new OperationAnnotationScanner();

		public <T> void process(Map<Class<?>, Object> models, TypeLiteral<T> typeLiteral, TypeEncounter<T> typeEncounter) {
			Object model = scanner.scan(typeLiteral.getRawType());
			if (model != null) {
				models.put(typeLiteral.getRawType(), model);
			}
		}

	}
}
