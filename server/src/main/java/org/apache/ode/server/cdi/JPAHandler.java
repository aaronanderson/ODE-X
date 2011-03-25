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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.util.AnnotationLiteral;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceContext;

public class JPAHandler extends Handler {

	public static class Inject extends AnnotationLiteral<javax.inject.Inject> implements javax.inject.Inject {
	};
	
	
	public static class  EntityManagerProducer {
		
		private Map<String, EntityManagerFactory> emfCache = new HashMap<String, EntityManagerFactory>();
		
		@Produces
		public EntityManager create(InjectionPoint ip){
			PersistenceContext pc = ip.getAnnotated().getAnnotation(PersistenceContext.class);
			if (pc !=null && pc.unitName() !=null){
				EntityManagerFactory emf = emfCache.get(pc.unitName());
				if (emf == null){
					emf = Persistence.createEntityManagerFactory(pc.unitName());
					emfCache.put(pc.unitName(), emf);
				}
				return emf.createEntityManager();
				
			}
			return null;
			
		}
		
		
		public void close(@Disposes EntityManager manager){
		  manager.close();	
		}
		
		@PostConstruct
		public void init() {
			//Can we prepopulate the cache?
		}
		
		@PreDestroy
		public void destroy() {
			for (EntityManagerFactory emf : emfCache.values()){
				emf.close();
			}
		}

	}

	@Override
	public void beforeBeanDiscovery(BeforeBeanDiscovery bbd, BeanManager bm) {
		bbd.addAnnotatedType(bm.createAnnotatedType(EntityManagerProducer.class));
	}

	@SuppressWarnings("unchecked")
	@Override
	public void processAnnotatedType(ProcessAnnotatedType<?> adv, BeanManager bm) {

		System.out.format("XXXXProcess Annotated type: %s \n", adv.getAnnotatedType().getJavaClass());
		if (scanForAnnotations(adv.getAnnotatedType())) {
			adv.setAnnotatedType(updateAnnotations(adv.getAnnotatedType()));
		}
	}

	@SuppressWarnings("unchecked")
	public boolean scanForAnnotations(AnnotatedType<?> type) {
		System.out.format("XXXXScanning Annotated type: %s \n", type.getJavaClass());
		// we will skip the class level/JNDI name annotation

		for (AnnotatedMethod<?> method : type.getMethods()) {

			if (method.isAnnotationPresent(PersistenceContext.class)) {
				return true;

			}
		}
		for (AnnotatedField<?> field : type.getFields()) {
			if (field.isAnnotationPresent(PersistenceContext.class)) {
				return true;
			}
		}

		return false;
	}

	@SuppressWarnings("unchecked")
	public AnnotatedType updateAnnotations(AnnotatedType<?> type) {
		AnnotatedTypeImpl<?> at = new AnnotatedTypeImpl(type);
		System.out.format("Processing Annotated type: %s \n", at.getJavaClass());
		for (AnnotatedMethod<?> method : at.getMethods()) {
			if (method.isAnnotationPresent(PersistenceContext.class)) {
				//PersistenceContext ctx = method.getAnnotation(PersistenceContext.class);
				System.out.format("************* Identified PersistenceContext annotation on method %s\n", method
						.getJavaMember().getName());
				method.getAnnotations().add(new Inject());
			}
		}
		for (AnnotatedField<?> field : at.getFields()) {
			if (field.isAnnotationPresent(PersistenceContext.class)) {
				//PersistenceContext ctx = field.getAnnotation(PersistenceContext.class);
				System.out.format("############# Identified PersistenceContext annotation on field %s\n", field.getJavaMember().getName());
				field.getAnnotations().add(new Inject());
			}
		}
		return at;
	}
	
	

}
