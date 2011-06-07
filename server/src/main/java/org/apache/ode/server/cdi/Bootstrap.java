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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.enterprise.inject.spi.ProcessObserverMethod;
import javax.enterprise.inject.spi.ProcessProducer;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Singleton;

import org.apache.ode.server.Server;
import org.apache.ode.server.xml.ServerType;
import org.apache.ode.spi.cdi.Handler;

public class Bootstrap implements Extension {
	// We will bootstrap all CDI beans rather than using beans.xml for
	// autodiscovery

	ServerType server;
	List<Handler> handlers = new ArrayList<Handler>();
	List<AnnotatedType<?>> addedTypes = new ArrayList<AnnotatedType<?>>();

	public Bootstrap() {

	}

	public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {
		try {
			server = Server.readConfig();
			for (String h : server.getHandlers().getHandlerClass()) {
				try {
					Class<Handler> handler = (Class<Handler>) Class.forName(h);
					if (handler != null) {
						handlers.add(handler.newInstance());
						System.out.format("Added handler %s\n", h);
					}

				} catch (Exception ce) {
					ce.printStackTrace();
				}
			}
		} catch (Exception ce) {
			ce.printStackTrace();
		}

		System.out.format("BeforeBeanDiscovery \n");
		BeforeBeanDiscovery bbdImpl = new BeforeBeanDiscoveryImpl(bbd);
		for (Handler h : handlers) {
			h.beforeBeanDiscovery(bbdImpl, bm);
		}
		/*
		 * Due to CDI spec FAIL https://issues.jboss.org/browse/CDI-33 and
		 * corresponding WELD issue https://issues.jboss.org/browse/WELD-796
		 * processAnnotatedType events are not fired for extension added
		 * annotated types (why extension added annotated types are excluded
		 * makes no sense and is probably a result of a strict interpretation of
		 * the spec rather than intention). This hack emulates a
		 * processAnnotatedType event invocation for each annotated type so
		 * special treatment of how the type was discovered can be avoided.
		 * Maybe this will be fixed in five years when CDI 1.1 is released,
		 * haha.
		 */
		for (AnnotatedType<?> t : addedTypes) {
			boolean vetoed= false;
			ProcessAnnotatedTypeImpl type = new ProcessAnnotatedTypeImpl(t);
			for (Handler h : handlers) {
				h.processAnnotatedType(type, bm);
				if (type.isVeto()) {
					vetoed = true;
					break;
				}
			}
			if (!vetoed){
			   bbd.addAnnotatedType(type.getAnnotatedType());
			}
		}
	}

	public void afterBeanDiscovery(@Observes AfterBeanDiscovery abd, BeanManager bm) {
		System.out.format("AfterBeanDiscovery \n");
		for (Handler h : handlers) {
			h.afterBeanDiscovery(abd, bm);
		}
		
		 AnnotatedType<ServerType> at = bm.createAnnotatedType(ServerType.class);
         final InjectionTarget<ServerType> it = bm.createInjectionTarget(at);
         
         Bean<ServerType> si = new Bean<ServerType>() {

             public Set<Type> getTypes() {
                 Set<Type> types = new HashSet<Type>();
                 types.add(ServerType.class);
                 types.add(Object.class);
                 return types;
             }

             public Set<Annotation> getQualifiers() {
                 Set<Annotation> qualifiers = new HashSet<Annotation>();
                 qualifiers.add(new AnnotationLiteral<Default>(){
             
                 });
                 return qualifiers;

             }

             public Class<? extends Annotation> getScope() {
                 return Singleton.class;
             }

             public String getName() {
                 return "ServerConfig";
             }

             public Set<Class<? extends Annotation>> getStereotypes() {
                 return Collections.EMPTY_SET;
             }

             public Class<?> getBeanClass() {
                 return ServerType.class;
             }

             public boolean isAlternative() {
                 return false;
             }

             public boolean isNullable() {
                 return false;
             }

             public Set<InjectionPoint> getInjectionPoints() {
                 return it.getInjectionPoints();
             }

             @Override
             public ServerType create(CreationalContext<ServerType> ctx) {
                 return server;

             }

             @Override
             public void destroy(ServerType instance,
                     CreationalContext<ServerType> ctx) {
             }
         };
         abd.addBean(si);

	}

	public void afterDeployment(@Observes AfterDeploymentValidation adv, BeanManager bm) {
		System.out.format("^^^^^^^^^^^^^^^^^^^^^^^^^AfterDeploymentValidation ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^\n");
		for (Handler h : handlers) {
			h.afterDeploymentValidation(adv, bm);
		}
	}

	public void beforeShutdown(@Observes BeforeShutdown bfs, BeanManager bm) {
		System.out.format("BeforeShutdown \n");
		//Reverse the order
		for (int i= handlers.size()-1; i>-1;i--){
			handlers.get(i).beforeShutdown(bfs, bm);
		}
	}

	public void processAnnotatedType(@Observes ProcessAnnotatedType<?> pa, BeanManager bm) {
		System.out.format("ProcessAnnotatedType \n");
		System.exit(0);
		for (Handler h : handlers) {
			h.processAnnotatedType(pa, bm);
		}
	}

	public void processInjectionTarget(@Observes ProcessInjectionTarget<?> pit, BeanManager bm) {
		System.out.format("ProcessInjectionTarget: class: %s points: %s\n", pit.getAnnotatedType(), pit.getInjectionTarget().getInjectionPoints());
		for (Handler h : handlers) {
			h.processInjectionTarget(pit, bm);
		}
	}

	public void processProducer(@Observes ProcessProducer<?, ?> ppd, BeanManager bm) {
		System.out.format("ProcessProducer: producer %s \n", ppd.getProducer());
		for (Handler h : handlers) {
			h.processProducer(ppd, bm);
		}
	}

	public void processBean(@Observes ProcessBean<?> pb, BeanManager bm) {
		System.out.format("ProcessBean: baseType: %s \n", pb.getAnnotated().getBaseType());
		for (Handler h : handlers) {
			h.processBean(pb, bm);
		}
	}

	public void processObserverMethod(@Observes ProcessObserverMethod<?, ?> pom, BeanManager bm) {
		System.out.format("ProcessObserverMethod \n");
		for (Handler h : handlers) {
			h.processObserverMethod(pom, bm);
		}
	}

	public class BeforeBeanDiscoveryImpl implements BeforeBeanDiscovery {

		BeforeBeanDiscovery impl;

		BeforeBeanDiscoveryImpl(BeforeBeanDiscovery impl) {
			this.impl = impl;
		}

		@Override
		public void addQualifier(Class<? extends Annotation> paramClass) {
			impl.addQualifier(paramClass);

		}

		@Override
		public void addScope(Class<? extends Annotation> paramClass, boolean paramBoolean1, boolean paramBoolean2) {
			impl.addScope(paramClass, paramBoolean1, paramBoolean2);

		}

		@Override
		public void addStereotype(Class<? extends Annotation> paramClass, Annotation... paramArrayOfAnnotation) {
			impl.addStereotype(paramClass, paramArrayOfAnnotation);

		}

		@Override
		public void addInterceptorBinding(Class<? extends Annotation> paramClass, Annotation... paramArrayOfAnnotation) {
			impl.addInterceptorBinding(paramClass, paramArrayOfAnnotation);

		}

		@Override
		public void addAnnotatedType(AnnotatedType<?> paramAnnotatedType) {
			addedTypes.add(paramAnnotatedType);

		}

	}

	

}
