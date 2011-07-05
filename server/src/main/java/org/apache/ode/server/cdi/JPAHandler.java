/*
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
package org.apache.ode.server.cdi;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import org.apache.ode.spi.cdi.Handler;

public class JPAHandler extends Handler {
	Bean<EntityManagerProducer> emfpBean;
	CreationalContext<EntityManagerProducer> emfpCtx;
	EntityManagerProducer emfp;

	private static final Logger log = Logger.getLogger(JPAHandler.class.getName());

	public static class Inject extends AnnotationLiteral<javax.inject.Inject> implements javax.inject.Inject {
	};

	@Singleton
	public static class EntityManagerProducer {

		private Map<String, EntityManagerFactory> emfCache = new HashMap<String, EntityManagerFactory>();

		@Produces
		@Dependent
		public EntityManager createEM(InjectionPoint ip) {
			PersistenceContext pc = ip.getAnnotated().getAnnotation(PersistenceContext.class);
			if (pc != null && pc.unitName() != null) {
				EntityManagerFactory emf = emfCache.get(pc.unitName());
				if (emf == null) {
					emf = Persistence.createEntityManagerFactory(pc.unitName());
					emfCache.put(pc.unitName(), emf);
				}
				EntityManager em = emf.createEntityManager();
				return em;

			}
			return null;

		}

		@Produces
		@Dependent
		public EntityManagerFactory createEMF(InjectionPoint ip) {
			PersistenceUnit pc = ip.getAnnotated().getAnnotation(PersistenceUnit.class);
			if (pc != null && pc.unitName() != null) {
				EntityManagerFactory emf = emfCache.get(pc.unitName());
				if (emf == null) {
					emf = Persistence.createEntityManagerFactory(pc.unitName());
					emfCache.put(pc.unitName(), emf);
				}
				return emf;
			}
			return null;

		}

		public void close(@Disposes EntityManager manager) {
			manager.close();
			log.finest("closed entitymanager");
		}

		@PostConstruct
		public void init() {
			// Can we prepopulate the cache?
		}

		@PreDestroy
		public void destroy() {
			for (EntityManagerFactory emf : emfCache.values()) {
				emf.close();
				log.finer("closed emf" + emf.toString());
			}
		}

	}

	@Override
	public void beforeBeanDiscovery(BeforeBeanDiscovery bbd, BeanManager bm) {
		bbd.addAnnotatedType(bm.createAnnotatedType(EntityManagerProducer.class));
	}

	@Override
	public void afterDeploymentValidation(AfterDeploymentValidation adv, BeanManager bm) {

		Set<Bean<?>> beans = bm.getBeans(EntityManagerProducer.class, new AnnotationLiteral<Any>() {
		});
		if (beans.size() > 0) {
			emfpBean = (Bean<EntityManagerProducer>) beans.iterator().next();
			emfpCtx = bm.createCreationalContext(emfpBean);
			emfp = (EntityManagerProducer) bm.getReference(emfpBean, EntityManagerProducer.class, emfpCtx);
		} else {
			log.log(Level.SEVERE, "Can't find class {0}", JPAHandler.class);
		}

	}

	@Override
	public void beforeShutdown(BeforeShutdown adv, BeanManager bm) {
		if (emfp != null) {
			emfpBean.destroy(emfp, emfpCtx);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void processAnnotatedType(ProcessAnnotatedType<?> adv, BeanManager bm) {

		log.log(Level.FINER, "Process Annotated type: {0}", adv.getAnnotatedType().getJavaClass());
		if (scanForAnnotations(adv.getAnnotatedType())) {
			adv.setAnnotatedType(updateAnnotations(adv.getAnnotatedType()));
		}
	}

	@SuppressWarnings("unchecked")
	public boolean scanForAnnotations(AnnotatedType<?> type) {
		log.log(Level.FINER, "Scanning Annotated type: {0}", type.getJavaClass());
		// we will skip the class level/JNDI name annotation

		for (AnnotatedMethod<?> method : type.getMethods()) {

			if (method.isAnnotationPresent(PersistenceContext.class)) {
				return true;
			} else if (method.isAnnotationPresent(PersistenceUnit.class)) {
				return true;
			}
		}
		for (AnnotatedField<?> field : type.getFields()) {
			if (field.isAnnotationPresent(PersistenceContext.class)) {
				return true;
			} else if (field.isAnnotationPresent(PersistenceUnit.class)) {
				return true;
			}
		}

		return false;
	}

	@SuppressWarnings("unchecked")
	public AnnotatedType updateAnnotations(AnnotatedType<?> type) {
		AnnotatedTypeImpl<?> at = new AnnotatedTypeImpl(type);
		log.log(Level.FINER, "Processing Annotated type: {0}", at.getJavaClass());
		for (AnnotatedMethod<?> method : at.getMethods()) {
			if (method.isAnnotationPresent(PersistenceContext.class)) {
				// PersistenceContext ctx =
				// method.getAnnotation(PersistenceContext.class);
				log.log(Level.FINER, "Identified PersistenceContext annotation on method {0}", method.getJavaMember().getName());
				method.getAnnotations().add(new Inject());
			} else if (method.isAnnotationPresent(PersistenceUnit.class)) {
				// PersistenceContext ctx =
				// method.getAnnotation(PersistenceContext.class);
				log.log(Level.FINER, "Identified PersistenceUnit annotation on method {0}", method.getJavaMember().getName());
				method.getAnnotations().add(new Inject());
			}
		}
		for (AnnotatedField<?> field : at.getFields()) {
			if (field.isAnnotationPresent(PersistenceContext.class)) {
				// PersistenceContext ctx =
				// field.getAnnotation(PersistenceContext.class);
				log.log(Level.FINER, "Identified PersistenceContext annotation on field {0}", field.getJavaMember().getName());
				field.getAnnotations().add(new Inject());
			} else if (field.isAnnotationPresent(PersistenceUnit.class)) {
				// PersistenceContext ctx =
				// field.getAnnotation(PersistenceContext.class);
				log.log(Level.FINER, "Identified PersistenceUnit annotation on field {0}", field.getJavaMember().getName());
				field.getAnnotations().add(new Inject());
			}
		}
		return at;
	}

}
