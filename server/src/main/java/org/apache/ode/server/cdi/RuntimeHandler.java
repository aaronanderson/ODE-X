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

import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.util.AnnotationLiteral;

import org.apache.ode.runtime.build.BuildSystem;
import org.apache.ode.runtime.wsdl.WSDL;
import org.apache.ode.runtime.xml.XML;
import org.apache.ode.runtime.xsd.XSD;
import org.apache.ode.runtime.xsl.XSL;
import org.apache.ode.spi.cdi.Handler;

public class RuntimeHandler extends Handler {

	Bean<XSD> xsdBean;
	CreationalContext<XSD> xsdCtx;
	XSD xsd;

	Bean<XML> xmlBean;
	CreationalContext<XML> xmlCtx;
	XML xml;

	Bean<WSDL> wsdlBean;
	CreationalContext<WSDL> wsdlCtx;
	WSDL wsdl;

	Bean<XSL> xslBean;
	CreationalContext<XSL> xslCtx;
	XSL xsl;

	Bean<BuildSystem> buildSysBean;
	CreationalContext<BuildSystem> buildSysCtx;
	BuildSystem buildSys;

	public void beforeBeanDiscovery(BeforeBeanDiscovery bbd, BeanManager bm) {
		bbd.addAnnotatedType(bm.createAnnotatedType(XSD.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(XML.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(WSDL.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(XSL.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(BuildSystem.class));
	}

	public void afterDeploymentValidation(AfterDeploymentValidation adv, BeanManager bm) {
		Set<Bean<?>> beans = bm.getBeans(XSD.class, new AnnotationLiteral<Any>() {
		});
		if (beans.size() > 0) {
			xsdBean = (Bean<XSD>) beans.iterator().next();
			xsdCtx = bm.createCreationalContext(xsdBean);
			xsd = (XSD) bm.getReference(xsdBean, XSD.class, xsdCtx);
		} else {
			System.out.println("Can't find class " + XSD.class);
		}

		beans = bm.getBeans(XML.class, new AnnotationLiteral<Any>() {
		});
		if (beans.size() > 0) {
			xmlBean = (Bean<XML>) beans.iterator().next();
			xmlCtx = bm.createCreationalContext(xmlBean);
			xml = (XML) bm.getReference(xmlBean, XML.class, xmlCtx);
		} else {
			System.out.println("Can't find class " + XML.class);
		}

		beans = bm.getBeans(WSDL.class, new AnnotationLiteral<Any>() {
		});
		if (beans.size() > 0) {
			wsdlBean = (Bean<WSDL>) beans.iterator().next();
			wsdlCtx = bm.createCreationalContext(wsdlBean);
			wsdl = (WSDL) bm.getReference(wsdlBean, WSDL.class, wsdlCtx);
		} else {
			System.out.println("Can't find class " + WSDL.class);
		}

		beans = bm.getBeans(XSL.class, new AnnotationLiteral<Any>() {
		});
		if (beans.size() > 0) {
			xslBean = (Bean<XSL>) beans.iterator().next();
			xslCtx = bm.createCreationalContext(xslBean);
			xsl = (XSL) bm.getReference(xslBean, XSL.class, xslCtx);
		} else {
			System.out.println("Can't find class " + XSL.class);
		}

		beans = bm.getBeans(BuildSystem.class, new AnnotationLiteral<Any>() {
		});
		if (beans.size() > 0) {
			buildSysBean = (Bean<BuildSystem>) beans.iterator().next();
			buildSysCtx = bm.createCreationalContext(buildSysBean);
			buildSys = (BuildSystem) bm.getReference(buildSysBean, BuildSystem.class, buildSysCtx);
		} else {
			System.out.println("Can't find class " + BuildSystem.class);
		}

	}

	@Override
	public void beforeShutdown(BeforeShutdown adv, BeanManager bm) {
		if (xsd != null) {
			xsdBean.destroy(xsd, xsdCtx);
		}

		if (xml != null) {
			xmlBean.destroy(xml, xmlCtx);
		}

		if (wsdl != null) {
			wsdlBean.destroy(wsdl, wsdlCtx);
		}

		if (xsl != null) {
			xslBean.destroy(xsl, xslCtx);
		}

		if (buildSys != null) {
			buildSysBean.destroy(buildSys, buildSysCtx);
		}
	}

}
