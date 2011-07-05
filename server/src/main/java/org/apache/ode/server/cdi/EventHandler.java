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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.event.Reception;
import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.util.AnnotationLiteral;

import org.apache.ode.spi.cdi.Handler;
import org.apache.ode.spi.event.Channel;
import org.apache.ode.spi.event.Publisher;
import org.apache.ode.spi.event.Subscriber;

public class EventHandler extends Handler {

	private static final Logger log = Logger.getLogger(EventHandler.class.getName());

	public static class Inject extends AnnotationLiteral<javax.inject.Inject> implements javax.inject.Inject {
	};

	public static class Observes extends AnnotationLiteral<javax.enterprise.event.Observes> implements javax.enterprise.event.Observes {
		TransactionPhase phase;
		Reception reception;

		public Observes() {
			this.phase = TransactionPhase.IN_PROGRESS;
			this.reception = Reception.ALWAYS;
		}

		@Override
		public TransactionPhase during() {
			return phase;
		}

		@Override
		public Reception notifyObserver() {
			return reception;
		}
	};

	public static class EventQualifierImpl extends AnnotationLiteral<EventQualifier> implements EventQualifier {
		String type;

		public EventQualifierImpl(String type) {
			this.type = type;
		}

		@Override
		public String value() {
			return type;
		}

	};

	@Default
	public static class ChannelImpl<T> implements Channel<T> {
		@javax.inject.Inject
		BeanManager mgr;
		String type;

		public ChannelImpl(String type, BeanManager mgr) {
			this.type = type;
			this.mgr = mgr;

		}

		@Override
		public void send(T event) {
			mgr.fireEvent(event, new EventQualifierImpl(type));
		}
	}

	public static class ChannelProducer<T> {

		@Produces
		public Channel<T> create(InjectionPoint ip, BeanManager mgr) {
			Publisher publisher = ip.getAnnotated().getAnnotation(Publisher.class);
			String type = "";
			if (publisher != null) {
				type = publisher.value();
			}
			return new ChannelImpl<T>(type, mgr);

		}

	}

	@Override
	public void beforeBeanDiscovery(BeforeBeanDiscovery bbd, BeanManager bm) {
		bbd.addAnnotatedType(bm.createAnnotatedType(ChannelProducer.class));
	}

	@SuppressWarnings("unchecked")
	@Override
	public void processAnnotatedType(ProcessAnnotatedType<?> adv, BeanManager bm) {

		log.log(Level.FINEST, "Process Annotated type: {0}", adv.getAnnotatedType().getJavaClass());
		if (scanForAnnotations(adv.getAnnotatedType())) {
			adv.setAnnotatedType(updateAnnotations(adv.getAnnotatedType()));
		}
	}

	@SuppressWarnings("unchecked")
	public boolean scanForAnnotations(AnnotatedType<?> type) {
		log.log(Level.FINEST, "Scanning Annotated type: {0}", type.getJavaClass());
		for (AnnotatedMethod<?> method : type.getMethods()) {
			for (AnnotatedParameter<?> param : method.getParameters()) {
				if (param.isAnnotationPresent(Subscriber.class)) {
					return true;
				}
			}
		}
		for (AnnotatedField<?> field : type.getFields()) {
			if (field.isAnnotationPresent(Publisher.class)) {
				Type t = field.getBaseType();
				if (t instanceof ParameterizedType && ((Class<?>) ((ParameterizedType) t).getRawType()).isAssignableFrom(Channel.class)) {
					return true;
				}
			}
		}

		return false;
	}

	@SuppressWarnings("unchecked")
	public AnnotatedType updateAnnotations(AnnotatedType<?> type) {
		AnnotatedTypeImpl<?> at = new AnnotatedTypeImpl(type);
		log.log(Level.FINEST, "Processing Annotated type: {0}", at.getJavaClass());
		for (AnnotatedMethod<?> method : at.getMethods()) {
			for (AnnotatedParameter<?> param : method.getParameters()) {
				if (param.isAnnotationPresent(Subscriber.class)) {
					Subscriber subscriber = param.getAnnotation(Subscriber.class);
					log.log(Level.FINEST, "Identified Subscriber annotation {0} on param type {1}", new Object[] { subscriber.value(), param.getBaseType() });
					param.getAnnotations().add(new Observes());
					if (subscriber.value().length() > 0) {
						param.getAnnotations().add(new EventQualifierImpl(subscriber.value()));
					}
				}
			}
		}
		for (AnnotatedField<?> field : at.getFields()) {
			if (field.isAnnotationPresent(Publisher.class)) {
				Type t = field.getBaseType();
				if (t instanceof ParameterizedType && ((Class<?>) ((ParameterizedType) t).getRawType()).isAssignableFrom(Channel.class)) {
					Publisher publisher = field.getAnnotation(Publisher.class);
					log.log(Level.FINEST, "Identified Publisher annotation {0} on field {1}", new Object[] { publisher.value(), t });
					field.getAnnotations().add(new Inject());
				}
			}
		}
		return at;
	}

}
