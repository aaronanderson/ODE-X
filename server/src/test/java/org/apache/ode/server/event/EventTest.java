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
package org.apache.ode.server.event;

import static org.junit.Assert.assertTrue;

import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;

import org.apache.ode.server.Server;
import org.apache.ode.server.cdi.EventHandler;
import org.apache.ode.server.cdi.StaticHandler;
import org.apache.ode.spi.cdi.Handler;
import org.apache.ode.spi.event.Channel;
import org.apache.ode.spi.event.Publisher;
import org.apache.ode.spi.event.Subscriber;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class EventTest {
	private static Server server;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		StaticHandler.clear();
		StaticHandler.addDelegate(new EventHandler());
		StaticHandler.addDelegate(new Handler() {
			
			@Override
			public void beforeBeanDiscovery(BeforeBeanDiscovery bbd, BeanManager bm) {				
				bbd.addAnnotatedType(bm.createAnnotatedType(SubscriberAllBean.class));
				bbd.addAnnotatedType(bm.createAnnotatedType(SubscriberBean.class));
				bbd.addAnnotatedType(bm.createAnnotatedType(SubscriberNonBean.class));
				bbd.addAnnotatedType(bm.createAnnotatedType(PublisherAllBean.class));
				bbd.addAnnotatedType(bm.createAnnotatedType(PublisherBean.class));
				bbd.addAnnotatedType(bm.createAnnotatedType(PublisherNonBean.class));
			}			
			
		});
		server = new Server();
		server.start();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		server.stop();
	}

	@Test
	public void testSubscriberAll() {
		SubscriberAllBean.reset();
		server.createEvent().select(MyEvent.class).fire(new MyEvent());
		assertTrue(SubscriberAllBean.hasFired());

	}

	@Test
	public void testSubscriberQualifier() {
		SubscriberBean.reset();
		server.createEvent().select(MyEvent.class, new EventHandler.EventQualifierImpl("MyEvent")).fire(new MyEvent());
		assertTrue(SubscriberBean.hasFired());
	}

	@Test
	public void testSubscriberNonQualifier() {
		SubscriberAllBean.reset();
		SubscriberBean.reset();
		SubscriberNonBean.reset();
		server.createEvent().select(MyEvent.class, new EventHandler.EventQualifierImpl("NonEvent")).fire(new MyEvent());
		assertTrue(SubscriberAllBean.hasFired());
		assertTrue(SubscriberNonBean.hasFired());
		assertTrue(!SubscriberBean.hasFired());
	}

	@Test
	public void testPublisherAll() {
		SubscriberAllBean.reset();
		Set<Bean<?>> beans = server.getBeanManager().getBeans(PublisherAllBean.class);
		Bean<PublisherAllBean> publisherBean = (Bean<PublisherAllBean>) beans.iterator().next();
		CreationalContext<PublisherAllBean> ctx = server.getBeanManager().createCreationalContext(publisherBean);
		PublisherAllBean reference = (PublisherAllBean) server.getBeanManager().getReference(publisherBean,
				PublisherAllBean.class, ctx);
		reference.trigger();
		assertTrue(SubscriberAllBean.hasFired());

	}

	@Test
	public void testPublisherQualifier() {
		SubscriberBean.reset();
		Set<Bean<?>> beans = server.getBeanManager().getBeans(PublisherBean.class);
		Bean<PublisherBean> publisherBean = (Bean<PublisherBean>) beans.iterator().next();
		CreationalContext<PublisherBean> ctx = server.getBeanManager().createCreationalContext(publisherBean);
		PublisherBean reference = (PublisherBean) server.getBeanManager().getReference(publisherBean,
				PublisherBean.class, ctx);
		reference.trigger();
		assertTrue(SubscriberBean.hasFired());
	}

	@Test
	public void testPublisherNonQualifier() {
		SubscriberAllBean.reset();
		SubscriberBean.reset();
		SubscriberNonBean.reset();

		Set<Bean<?>> beans = server.getBeanManager().getBeans(PublisherNonBean.class);
		Bean<PublisherNonBean> publisherNonBean = (Bean<PublisherNonBean>) beans.iterator().next();
		CreationalContext<PublisherNonBean> ctx = server.getBeanManager().createCreationalContext(publisherNonBean);
		PublisherNonBean reference = (PublisherNonBean) server.getBeanManager().getReference(publisherNonBean,
				PublisherNonBean.class, ctx);
		reference.trigger();
		assertTrue(SubscriberAllBean.hasFired());
		assertTrue(SubscriberNonBean.hasFired());
		assertTrue(!SubscriberBean.hasFired());

	}

	public static class MyEvent {

	}

	public static class PublisherAllBean {

		@Publisher
		Channel<MyEvent> channel;

		public void trigger() {
			channel.send(new MyEvent());
		}
	}

	public static class PublisherBean {

		@Publisher("MyEvent")
		Channel<MyEvent> channel;

		public void trigger() {
			channel.send(new MyEvent());
		}
	}

	public static class PublisherNonBean {

		@Publisher("NonEvent")
		Channel<MyEvent> channel;

		public void trigger() {
			channel.send(new MyEvent());
		}
	}

	public static class SubscriberAllBean {

		private static boolean fired = false;

		public static boolean hasFired() {
			return fired;
		}

		public static void reset() {
			fired = false;
		}

		public void handleResponse(@Subscriber MyEvent response) {
			fired = true;
		}
	}

	public static class SubscriberBean {

		private static boolean fired = false;

		public static boolean hasFired() {
			return fired;
		}

		public static void reset() {
			fired = false;
		}

		public void handleResponse(@Subscriber("MyEvent") MyEvent response) {
			fired = true;
		}
	}

	public static class SubscriberNonBean {

		private static boolean fired = false;

		public static boolean hasFired() {
			return fired;
		}

		public static void reset() {
			fired = false;
		}

		public void handleResponse(@Subscriber("NonEvent") MyEvent response) {
			fired = true;
		}
	}

}
