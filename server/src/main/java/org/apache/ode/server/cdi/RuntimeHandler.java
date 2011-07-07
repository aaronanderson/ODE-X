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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.management.ObjectName;

import org.apache.ode.runtime.build.BuildExecutor;
import org.apache.ode.runtime.build.BuildSystem;
import org.apache.ode.runtime.build.CompilersImpl;
import org.apache.ode.runtime.xsd.XMLSchemaContextImpl;
import org.apache.ode.runtime.exec.Exec;
import org.apache.ode.runtime.exec.platform.ActionExecutor;
import org.apache.ode.runtime.exec.platform.ActionPoll;
import org.apache.ode.runtime.exec.platform.Cluster;
import org.apache.ode.runtime.exec.platform.Cluster.ClusterId;
import org.apache.ode.runtime.exec.platform.Cluster.NodeId;
import org.apache.ode.runtime.exec.platform.HealthCheck;
import org.apache.ode.runtime.exec.platform.InstallMasterAction;
import org.apache.ode.runtime.exec.platform.InstallSlaveAction;
import org.apache.ode.runtime.exec.platform.PlatformImpl;
import org.apache.ode.runtime.jmx.BuildSystemImpl;
import org.apache.ode.runtime.wsdl.WSDL;
import org.apache.ode.runtime.wsdl.WSDLContextImpl;
import org.apache.ode.runtime.xml.XML;
import org.apache.ode.runtime.xsd.XSD;
import org.apache.ode.runtime.xsl.XSL;
import org.apache.ode.server.JMXServer;
import org.apache.ode.server.WSDLComponentImpl;
import org.apache.ode.server.xml.ServerConfig;
import org.apache.ode.spi.cdi.Handler;
import org.apache.ode.spi.repo.XMLValidate;

public class RuntimeHandler extends Handler {

	/*
	 * @Singleton public static class CompilerProducer {
	 * 
	 * @Produces
	 * 
	 * @Dependent public XMLSchemaContext createXMLSchemaCtx() { return new
	 * XMLSchemaContextImpl(); }
	 * 
	 * @Produces
	 * 
	 * @Dependent public WSDLContext createWSDLCtx() { return new
	 * WSDLContextImpl(); }
	 * 
	 * }
	 */

	public void beforeBeanDiscovery(BeforeBeanDiscovery bbd, BeanManager bm) {
		bbd.addAnnotatedType(bm.createAnnotatedType(XSD.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(XML.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(WSDL.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(XSL.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(XMLValidate.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(BuildSystem.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(BuildSystemImpl.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(BuildExecutor.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(CompilersImpl.class));
		// bbd.addAnnotatedType(bm.createAnnotatedType(CompilerProducer.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(XMLSchemaContextImpl.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(WSDLContextImpl.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(WSDLComponentImpl.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(Exec.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(PlatformImpl.class));
		bbd.addAnnotatedType(getClusterType(bm.createAnnotatedType(Cluster.class)));
		bbd.addQualifier(ClusterId.class);
		bbd.addQualifier(NodeId.class);
		bbd.addAnnotatedType(bm.createAnnotatedType(ClusterProducer.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(HealthCheck.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(ActionPoll.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(ActionExecutor.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(InstallMasterAction.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(InstallSlaveAction.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(org.apache.ode.runtime.jmx.PlatformImpl.class));
	}

	public void afterDeploymentValidation(AfterDeploymentValidation adv, BeanManager bm) {
		manage(XSD.class);
		manage(XSD.class);
		manage(XML.class);
		manage(WSDL.class);
		manage(XSL.class);
		manage(Cluster.class);
		manage(Exec.class);
		manage(BuildSystem.class);
		manage(BuildSystemImpl.class);
		manage(org.apache.ode.runtime.jmx.PlatformImpl.class);
		start(bm);

		Set<Bean<?>> beans = bm.getBeans(JMXServer.class, new AnnotationLiteral<Any>() {
		});
		if (beans.size() > 0) {
			Bean<?> bean = beans.iterator().next();
			JMXServer server = (JMXServer) bm.getReference(bean, JMXServer.class, bm.createCreationalContext(bean));
			try {
				server.getMBeanServer().registerMBean(getInstance(BuildSystemImpl.class), ObjectName.getInstance(BuildSystemImpl.OBJECTNAME));
				server.getMBeanServer().registerMBean(getInstance(org.apache.ode.runtime.jmx.PlatformImpl.class),
						ObjectName.getInstance(org.apache.ode.runtime.jmx.PlatformImpl.OBJECTNAME));
			} catch (Exception e) {
				e.printStackTrace();
				adv.addDeploymentProblem(e);
			}
		}

	}

	@Override
	public void beforeShutdown(BeforeShutdown adv, BeanManager bm) {
		stop();
	}

	public static class ClusterProducer {

		@Inject
		ServerConfig serverConfig;

		@Produces
		@ClusterId
		public String createClusterId() {
			return serverConfig.getClusterId();
		}

		@Produces
		@NodeId
		public String createNodeId() {
			String nodeId = serverConfig.getNodeId();
			if (nodeId != null) {
				return nodeId;
			}
			try {
				InetAddress addr = InetAddress.getLocalHost();
				nodeId = addr.getHostName();
				int index = nodeId.indexOf('.');
				if (index > -1) {
					nodeId = nodeId.substring(0, index);
				}
				return nodeId;
			} catch (UnknownHostException e) {
			}
			return "node1";
		}
	}

	public AnnotatedType<Cluster> getClusterType(AnnotatedType<Cluster> type) {
		AnnotatedTypeImpl<Cluster> at = new AnnotatedTypeImpl<Cluster>(type);
		final class Inject extends AnnotationLiteral<javax.inject.Inject> implements javax.inject.Inject {
		}
		for (AnnotatedField<?> field : at.getFields()) {
			if (field.isAnnotationPresent(ClusterId.class)) {
				field.getAnnotations().add(new Inject());
				// field.getAnnotations().add(new ClusterIdAnnotation());
			} else if (field.isAnnotationPresent(NodeId.class)) {
				field.getAnnotations().add(new Inject());
				// field.getAnnotations().add(new NodeIdAnnotation());
			}
		}
		return at;
	}
}
