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
package org.apache.ode.runtime.exec.platform;

import java.lang.reflect.Field;

import javax.jms.Session;
import javax.jms.Topic;

import org.apache.ode.runtime.exec.platform.JMSUtil.SessionImpl;
import org.apache.ode.runtime.exec.platform.JMSUtil.TopicImpl;
import org.apache.ode.runtime.exec.platform.NodeImpl.ActionRequest;
import org.apache.ode.runtime.exec.platform.NodeImpl.ActionResponse;
import org.apache.ode.runtime.exec.platform.NodeImpl.NodeCheck;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.MembersInjector;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

public class JMSModule extends AbstractModule {
	
	protected void configure() {

		bindListener(Matchers.any(), new JMSTypeListener());

		SessionImpl hSession = new SessionImpl();
		TopicImpl hTopic = new TopicImpl("ODE_HEALTHCHECK");
		bind(Session.class).annotatedWith(NodeCheck.class).toInstance(hSession);
		bind(Topic.class).annotatedWith(NodeCheck.class).toInstance(hTopic);

		SessionImpl arqSession = new SessionImpl();
		TopicImpl arqTopic = new TopicImpl("ODE_ACTION_REQUEST");
		bind(Session.class).annotatedWith(ActionRequest.class).toInstance(arqSession);
		bind(Topic.class).annotatedWith(ActionRequest.class).toInstance(arqTopic);

		SessionImpl arsSession = new SessionImpl();
		TopicImpl arsTopic = new TopicImpl("ODE_ACTION_RESPONSE");
		bind(Session.class).annotatedWith(ActionResponse.class).toInstance(arsSession);
		bind(Topic.class).annotatedWith(ActionResponse.class).toInstance(arsTopic);
	}

	public static class JMSTypeListener implements TypeListener {
		public <T> void hear(TypeLiteral<T> typeLiteral, TypeEncounter<T> typeEncounter) {
			for (Field field : typeLiteral.getRawType().getDeclaredFields()) {
				if (field.getType() == Session.class || field.getType() == Topic.class) {
					if (field.isAnnotationPresent(NodeCheck.class)) {
						typeEncounter.register(new JMSSessionMembersInjector(field,typeEncounter.getProvider(Key.get(field.getType(),NodeCheck.class))));
					}
				} /*else if (field.getType() == Topic.class) {
					if (field.isAnnotationPresent(NodeCheck.class)) {
						typeEncounter.register(typeEncounter.getMembersInjector(typeLiteral));
					}
				}*/

			}
		}
	}

	public static class JMSSessionMembersInjector<T> implements MembersInjector<T> {
		private final Field field;
		private final Provider provider;

		JMSSessionMembersInjector(Field field, Provider provider) {
			this.field = field;
			this.provider = provider;
			field.setAccessible(true);
		}

		public void injectMembers(T t) {
			try {
				field.set(t, provider.get());
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
	}

}
